package com.vigza.markweave.infrastructure.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigza.markweave.api.dto.Websocket.WsMessage;
import com.vigza.markweave.core.service.AlgorithmService;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.infrastructure.config.RabbitMqConfig;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

@Component
public class RetryMessageListener {

    @Autowired
    private AlgorithmService algorithmSerivce;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.RETRY_QUEUE)
    public void onMessage(String messagePayload){
        WsMessage<Object> msg;
        try {
            msg = objectMapper.readValue(messagePayload,WsMessage.class);
            Long docId = msg.getDocId();
            algorithmSerivce.processOperation(docId, msg);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
