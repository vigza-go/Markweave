package com.vigza.markweave.api.websocket;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.vigza.markweave.api.dto.Websocket.WsMessage;
import com.vigza.markweave.core.service.AlgorithmService;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.core.service.FileSystemServiceImpl;
import com.vigza.markweave.infrastructure.service.RedisService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
// 分布式条件下，由于Ot算法计算时会并发冲突，所以我们要上Redis分布式锁
// 分布式条件下，还需要考虑把本机的Ot计算结果通过RabbitMq广播给其他服务器上服务的用户
// 也就说一台服务器同时承担 操作变换计算 和 消息传递 的功能
public class CollaborationHandler extends TextWebSocketHandler {

    Map<Long, Set<WebSocketSession>> map = new ConcurrentHashMap<>();

    @Autowired
    private RedisService redisService;

    @Autowired
    private FileSystemServiceImpl fsService;

    @Autowired
    private CollaborationService collaborationService;

    @Autowired
    private AlgorithmService algorithmSerivce;

    /**
     * 解决又广播时前同一个session还没处理完的问题
     * 
     * @param session
     * @param message
     * @throws Exception
     */
    private void safeSend(WebSocketSession session, TextMessage message) throws Exception {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("Error sending message to session: {}", session.getId(), e);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received message: {} from session: {}", message.getPayload(), session.getId());
        WsMessage<JSONObject> clientMsg = JSONUtil.toBean(message.getPayload(),
                new TypeReference<WsMessage<JSONObject>>() {
                }, true);
        Long docId = clientMsg.getDocId();
        String token = (String) session.getAttributes().get("token");
        session.getAttributes().put("docId", docId);

        boolean added = map.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session);
        if (added) {
            redisService.incrementDocConnections(docId);
        }
        if (!collaborationService.canRead(token, docId)) {
            clientMsg.setMethod("error");
            clientMsg.setData(new JSONObject().set("msg", "无权访问"));
            safeSend(session, new TextMessage(JSONUtil.toJsonStr(clientMsg)));
            return;
        }

        String method = clientMsg.getMethod();
        // 1.处理获取全文请求
        if ("getNew".equals(method)) {
            clientMsg.setMethod("fullText");
            clientMsg.setData(new JSONObject().set("text", fsService.getDocContent(docId)));
            clientMsg.setVersion(redisService.getVersion(docId));
            safeSend(session, new TextMessage(JSONUtil.toJsonStr(clientMsg)));
            return;
        }

        // 2.处理拉取历史请求
        if ("pullHistory".equals(method)) {
            Long clientVer = clientMsg.getVersion();
            Long maxVer = redisService.getVersion(docId);
            Long size = redisService.getHistoryListSize(docId);
            if (maxVer - clientVer > size) {
                clientMsg.setMethod("fullText");
                clientMsg.setData(new JSONObject(fsService.getDocContent(docId)));
                clientMsg.setVersion(redisService.getVersion(docId));
                safeSend(session, new TextMessage(clientMsg.toString()));
                return;
            }
            List<String> historyList = redisService
                    .getHistoryRange(docId, size - (maxVer - clientVer), size - 1)
                    .stream()
                    .map(obj -> obj.toString()).collect(Collectors.toList());
            historyList.forEach(str -> {
                try {
                    safeSend(session, new TextMessage(str));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
            return;
        }
        
        // 3. 处理编辑操作，计算出op，然后广播
        // 消息的幂等处理
        if(redisService.tryProcessMessage(docId, clientMsg.getClientId(), clientMsg.getMsgId())){
            algorithmSerivce.processOperation(docId, clientMsg);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long docId = (Long) session.getAttributes().get("docId");
        /**
         * 风险：这是经典的 Check-then-Act 问题。
         * 假设 A、B 两个用户同时断开连接，
         * 两个线程可能同时判断 isEmpty() 为真，导致逻辑执行两次。
         * 更糟糕的是，如果此时正好有 C 用户进来，他的 Session 可能会被你的 clearRoom 给误删掉。
         */
        if (docId != null) {
            Set<WebSocketSession> sessions = map.get(docId);
            if (sessions != null) {
                boolean removed = sessions.remove(session);
                if (removed) {
                    redisService.decrementDocConnections(docId);
                    if (redisService.getDocConnections(docId) <= 0) {
                        String finalTarget = fsService.getDocContent(docId);
                        if (finalTarget != null) {
                            log.info("保存文档{}", docId);
                            fsService.updateDocContent(docId, finalTarget);
                            redisService.clearRoom(docId);
                        }
                        map.remove(docId);
                    }
                }
            }
        }

    }

    public void boardcastToLocal(Long docId, String messagePayload, String senderClientId) {
        Set<WebSocketSession> sessions = map.get(docId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    String clientId = (String) session.getAttributes().get("clientId");
                    // 不要发给自己
                    // if (!clientId.equals(senderClientId)) {
                    safeSend(session, new TextMessage(messagePayload));
                    // }
                } catch (Exception e) {
                    log.error("广播消息到本地session失败，sessionId：{}", session.getId(), e);
                }
            }
        }
    }

}
