package com.vigza.markweave.infrastructure.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vigza.markweave.core.service.AlgorithmService;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.infrastructure.config.RabbitMqConfig;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

@Component
public class RetryMessageListener {

    @Autowired
    private AlgorithmService algorithmSerivce;

    @RabbitListener(queues = RabbitMqConfig.RETRY_QUEUE)
    public void onMessage(String messagePayload){
        JSONObject msg = new JSONUtil().parseObj(messagePayload);
        Long docId = msg.getLong("docId");
        algorithmSerivce.processOperation(docId, msg);
    }
}
