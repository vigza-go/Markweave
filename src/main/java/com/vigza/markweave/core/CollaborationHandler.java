package com.vigza.markweave.core;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CollaborationHandler extends TextWebSocketHandler {
    // 之后需要改用redis
    private final AtomicInteger currentVersion = new AtomicInteger(0);

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    private final List<JSONObject> history = new CopyOnWriteArrayList<>();

    private String fullText = new String();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }



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

        String method = clientMsg.getStr("method");
        // 1.处理获取全文请求
        if ("get_new".equals(method)) {
            JSONObject mtdMsg = new JSONObject();
            mtdMsg.set("method", "get_new");
            mtdMsg.set("text", fullText.toString());
            mtdMsg.set("version", currentVersion.get());
            safeSend(session, new TextMessage(mtdMsg.toString()));
            return;
        }

        // 2.处理拉取历史请求
        if ("pull_history".equals(method)) {
            int clientVer = clientMsg.getInt("version");
            int maxVer = currentVersion.get();
            for (int i = clientVer + 1; i <= maxVer; i++) {
                safeSend(session, new TextMessage(history.get(i - 1).toString()));
            }
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
        synchronized (this) {
            int clientVer = clientMsg.getInt("version");
            TextOperation clientOp = null;
            if (clientVer < currentVersion.get()) {
                // 我们变换的时候，需要取出client的op，然后和历史op，进行transform，然后获得op'
                clientOp = new TextOperation().fromJSON(clientMsg.getJSONArray("op").toString());
                
                // 我们广播的version的就是它的操作clientaVer，所以clientVer执行过，这里应该从 + 1 开始
                for (int i = clientVer + 1; i <= currentVersion.get(); i++) {
                    if(history.get(i - 1).getStr("clientId").compareTo(clientMsg.getStr("clientId")) < 0){
                        TextOperation historyOp = new TextOperation().fromJSON(history.get(i - 1).getJSONArray("op").toString());
                        TextOperation[] transformed = TextOperation.transform(historyOp, clientOp);
                        clientOp = transformed[1];
                    }else{
                        TextOperation historyOp = new TextOperation().fromJSON(history.get(i - 1).getJSONArray("op").toString());
                        TextOperation[] transformed = TextOperation.transform(clientOp, historyOp);
                        clientOp = transformed[0];
                    }
                }
                clientMsg.set("op",clientOp.toJSON());
            }

            currentVersion.incrementAndGet();
            clientMsg.set("version", currentVersion.get());
            history.add(clientMsg);
            clientOp = new TextOperation().fromJSON(clientMsg.getStr("op"));
            // 我们拿着这个op然后apply到fullText上
            fullText = clientOp.apply(fullText.toString());

            String response = JSONUtil.toJsonStr(clientMsg);
            for (WebSocketSession s : sessions) {
                // 暂且不考虑断开连接的情况
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(response));
                }
            }
        }
    }

}
