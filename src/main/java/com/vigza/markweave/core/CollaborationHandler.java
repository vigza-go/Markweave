package com.vigza.markweave.core;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.vigza.markweave.infrastructure.service.RedisService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CollaborationHandler extends TextWebSocketHandler {

    Map<Long, Set<WebSocketSession>> map = new ConcurrentHashMap<>();

    @Autowired
    private RedisService redisService;

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
        JSONObject clientMsg = JSONUtil.parseObj(message.getPayload());
        Long docId = clientMsg.getLong("docId");
        session.getAttributes().put("docId",docId);
        synchronized (docId.toString().intern()) {
            map.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session);

            String method = clientMsg.getStr("method");
            // 1.处理获取全文请求
            if ("get_new".equals(method)) {
                JSONObject mtdMsg = new JSONObject();
                mtdMsg.set("method", "get_new");
                mtdMsg.set("text", redisService.getFullText(docId));
                mtdMsg.set("version", redisService.getVersion(docId));
                safeSend(session, new TextMessage(mtdMsg.toString()));
                return;
            }

            // 2.处理拉取历史请求
            if ("pull_history".equals(method)) {
                Long clientVer = clientMsg.getLong("version");
                Long maxVer = redisService.getVersion(docId);
                Long size = redisService.getHistoryListSize(docId);
                if (maxVer - clientVer > size) {
                    JSONObject msg = new JSONObject();
                    msg.set("info", "need_get_new");
                    safeSend(session, new TextMessage(msg.toString()));
                    return;
                }
                List<String> historyList = redisService.getHistoryRange(docId, size - (maxVer - clientVer), size - 1)
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

            // 3. 处理编辑操作 (核心同步块)
            // 线程 A 收到消息，执行到 currentVersion.incrementAndGet()，版本号从 141 变成 142。
            // 就在此时（还没执行 history.add），线程 B 收到另一条消息。
            // 线程 B 执行到 for(int i = clientVer + 1; i <= currentVersion.get(); i++)。
            // 此时线程 B 读到的 currentVersion 是 142。
            // 线程 B 尝试执行 history.get(142 - 1) 即 history.get(141)。
            // 但因为线程 A 的 history.add 还没跑完，history 列表的最大索引还是 140。
            // 砰！ ArrayIndexOutOfBoundsException 抛出。
            // 待优化，考虑用消息队列来避免锁的同步
            Long clientVer = clientMsg.getLong("version");
            TextOperation clientOp = null;
            Long currentVersion = redisService.getVersion(docId);
            if (clientVer < currentVersion) {
                // 我们变换的时候，需要取出client的op，然后和历史op，进行transform，然后获得op'
                clientOp = new TextOperation().fromJSON(clientMsg.getJSONArray("op").toString());

                // 我们广播的version的就是它的操作clientaVer，所以clientVer执行过，这里应该从 + 1 开始
                Long size = redisService.getHistoryListSize(docId);
                List<String> historyList = redisService
                        .getHistoryRange(docId, size - (currentVersion - clientVer), size - 1)
                        .stream()
                        .map(obj -> obj.toString()).collect(Collectors.toList());
                for (String history : historyList) {
                    JSONObject hisObj = JSONUtil.parseObj(history);

                    if (hisObj.getStr("clientId").compareTo(clientMsg.getStr("clientId")) < 0) {
                        TextOperation historyOp = new TextOperation()
                                .fromJSON(hisObj.getJSONArray("op").toString());
                        TextOperation[] transformed = TextOperation.transform(historyOp, clientOp);
                        clientOp = transformed[1];
                    } else {
                        TextOperation historyOp = new TextOperation()
                                .fromJSON(hisObj.getJSONArray("op").toString());
                        TextOperation[] transformed = TextOperation.transform(clientOp, historyOp);
                        clientOp = transformed[0];
                    }

                }
                clientMsg.set("op", clientOp.toJSON());
            }

            currentVersion = redisService.getAndIncrementVersion(docId);
            clientMsg.set("version", currentVersion);
            redisService.pushHistory(docId, clientMsg.toString());
            clientOp = new TextOperation().fromJSON(clientMsg.getStr("op"));
            // 我们拿着这个op然后apply到fullText上
            String fullText = redisService.getFullText(docId);
            fullText = clientOp.apply(fullText.toString());
            redisService.setFullText(docId, fullText);

            String response = JSONUtil.toJsonStr(clientMsg);
            for (WebSocketSession s : map.get(docId)) {
                // 暂且不考虑断开连接的情况
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(response));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long docId = (Long) session.getAttributes().get("docId");
        if(docId != null){
            map.get(docId).remove(session);
        }
    }
}
