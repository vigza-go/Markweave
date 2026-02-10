package com.vigza.markweave.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigza.markweave.api.dto.Websocket.WsMessage;
import com.vigza.markweave.common.util.IdGenerator;
import com.vigza.markweave.common.util.TextOperation;
import com.vigza.markweave.infrastructure.config.RabbitMqConfig;
import com.vigza.markweave.infrastructure.persistence.mapper.FsNodeMapper;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vigza.markweave.infrastructure.service.RedisService;

@Slf4j
@Service
public class AlgorithmServiceImpl implements AlgorithmService {

    @Autowired
    private FileSystemService fsService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void processOperation(Long docId, WsMessage<Object> clientMsg) {

        String lockKey = "lock:doc:" + docId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        log.info("processOp");
        try {
            isLocked = lock.tryLock(3, TimeUnit.SECONDS);
            if (isLocked) {
                log.info("isLocked");
                Long clientVer = clientMsg.getVersion();
                TextOperation clientOp = null;
                Long currentVersion = redisService.getVersion(docId);
                if (clientVer < currentVersion) {
                    // 我们变换的时候，需要取出client的op，然后和历史op，进行transform，然后获得op'
                    clientOp = new TextOperation()
                            .fromJSON(JSONUtil.parseObj(clientMsg.getData()).getJSONArray("op").toString());

                    // 我们广播的version的就是它的操作clientaVer，所以clientVer执行过，这里应该从 + 1 开始
                    Long size = redisService.getHistoryListSize(docId);
                    List<String> historyList = redisService
                            .getHistoryRange(docId, size - (currentVersion - clientVer), size - 1)
                            .stream()
                            .map(obj -> obj.toString()).collect(Collectors.toList());
                    for (String history : historyList) {
                        JSONObject hisObj = JSONUtil.parseObj(history);

                        if (hisObj.getStr("clientId").compareTo(clientMsg.getClientId()) < 0) {
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
                    clientMsg.setData(new JSONObject().set("op", clientOp.toJSON()));
                }

                try {
                    currentVersion = redisService.getAndIncrementVersion(docId);
                    clientMsg.setVersion(currentVersion);
                    redisService.pushHistory(docId, objectMapper.writeValueAsString(clientMsg));
                    clientOp = new TextOperation()
                            .fromJSON(JSONUtil.parseObj(clientMsg.getData()).getJSONArray("op").toString());
                    // 我们拿着这个op然后apply到fullText上
                    String fullText = fsService.getDocContent(docId);
                    fullText = clientOp.apply(fullText.toString());
                    redisService.setFullText(docId, fullText);
                    String response = objectMapper.writeValueAsString(clientMsg);
                    rabbitTemplate.convertAndSend(RabbitMqConfig.COLLABORATION_EXCHANGE, "", response);
                    log.info("send msg to collaboration exchange: {}", response);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            } else {
                log.warn("文档 {} 竞争激烈，转发至重试队列", docId);
                Integer count = clientMsg.getRetryCount();
                if (count == null) {
                    count = 0;
                }
                Integer retryCount = count + 1;
                clientMsg.setRetryCount(retryCount);
                rabbitTemplate.convertAndSend(RabbitMqConfig.RETRY_EXCHANGE, RabbitMqConfig.RETRY_ROUTING_KEY,
                        clientMsg, msg -> {
                            Long backoff = computeBackoff(retryCount);
                            msg.getMessageProperties().setExpiration(backoff.toString());
                            return msg;
                        });
            }
        } catch (InterruptedException e) {
            log.error("获取文档 {} 锁失败: {}", docId, e.getMessage());
        } finally {
            if (isLocked) {
                log.info("unlock");
                lock.unlock();
            }
        }
    }

    private Long computeBackoff(Integer retryCount) {
        int shift = Math.min(retryCount - 1, 16);
        return (1L << shift);
    }

}
