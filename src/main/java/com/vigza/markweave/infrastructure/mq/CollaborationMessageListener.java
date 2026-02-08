package com.vigza.markweave.infrastructure.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vigza.markweave.api.websocket.CollaborationHandler;
import com.vigza.markweave.infrastructure.config.RabbitMqConfig;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CollaborationMessageListener {
    @Autowired
    private CollaborationHandler collaborationHandler;

    @RabbitListener(queues = RabbitMqConfig.COLLABORATION_MSG_QUEUE)
    public void onMessage(String messagePayload){
        try{
            log.info("Received collaboration message: {}", messagePayload);
            JSONObject msg = new JSONUtil().parseObj(messagePayload);
            Long docId = msg.getLong("docId"); 
            String senderClientId = msg.getStr("clientId");

            collaborationHandler.boardcastToLocal(docId,messagePayload,senderClientId);
        }catch (Exception e){
            log.error("处理协作广播消息失败",e);
        }

    }

}
