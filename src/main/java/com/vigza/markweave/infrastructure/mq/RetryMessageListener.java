package com.vigza.markweave.infrastructure.mq;

import com.vigza.markweave.common.Constants;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.infrastructure.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RetryMessageListener {
    @Autowired
    private CollaborationService collaborationService;

    @RabbitListener(queues = RabbitMqConfig.RETRY_QUEUE)
    public void onRetryMessage(String messagePayload) {
        try {
            JSONObject msg = new JSONUtil().parseObj(messagePayload);
            Long docId = msg.getLong("docId");
            Integer retryCount = msg.getInt("retryCount", 0);
            if (retryCount >= Constants.Retry.MAX_ATTEMPTS) {
                log.warn("文档 {} 重试次数已达上限，丢弃消息", docId);
                return;
            }
            collaborationService.processOperation(docId, msg);
        } catch (Exception e) {
            log.error("处理重试消息失败", e);
        }
    }
}
