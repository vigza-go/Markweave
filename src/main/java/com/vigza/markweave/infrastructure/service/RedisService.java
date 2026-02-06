package com.vigza.markweave.infrastructure.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long expiration = 604800L;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String DOC_VERSION_PREFIX = "doc:version:";
    private static final String DOC_TEXT_PREFIX = "doc:text:";
    private static final String DOC_HISTORY_PREFIX = "doc:history:";
    private static final String DOC_CONNECTIONS_PREFIX = "doc:connections:";

    public Long getAndIncrementVersion(Long docId) {
        return redisTemplate.opsForValue().increment(DOC_VERSION_PREFIX + docId);
    }

    public Long getVersion(Long docId) {
        Object version = redisTemplate.opsForValue().get(DOC_VERSION_PREFIX + docId);
        return version == null ? 0L : (Long) version;
    }

    public String getFullText(Long docId) {
        Object text = redisTemplate.opsForValue().get(DOC_TEXT_PREFIX + docId);
        return text == null ? "" : text.toString();
    }

    public void setFullText(Long docId, String fullText) {
        redisTemplate.opsForValue().set(DOC_TEXT_PREFIX + docId, fullText);
    }

    public void pushHistory(Long docId, String opJson) {
        redisTemplate.opsForList().rightPush(DOC_HISTORY_PREFIX + docId, opJson);
        // 限制留存操作数量
        redisTemplate.opsForList().trim(DOC_HISTORY_PREFIX + docId, -500, -1);
    }

    public List<Object> getHistoryRange(Long docId, long l, long r) {
        return redisTemplate.opsForList().range(DOC_HISTORY_PREFIX + docId, l, r);
    }

    public Long getHistoryListSize(Long docId) {
        return redisTemplate.opsForList().size(DOC_HISTORY_PREFIX + docId);
    }

    public void addToBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expiration, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    public void clearRoom(Long docId) {
        String verKey = DOC_VERSION_PREFIX + docId;
        String textKey = DOC_TEXT_PREFIX + docId;
        String histKey = DOC_HISTORY_PREFIX + docId;
        redisTemplate.delete(Arrays.asList(verKey, textKey, histKey));
    }

    public Long getDocConnections(Long docId) {
        Object obj = redisTemplate.opsForValue().get(DOC_CONNECTIONS_PREFIX + docId);
        if (obj == null) {
            return 0L;
        }
        Long count = (Long) obj;
        if (count < 0) {
            redisTemplate.opsForValue().set(DOC_CONNECTIONS_PREFIX + docId, 0L);
        }
        redisTemplate.expire(DOC_CONNECTIONS_PREFIX + docId, expiration, TimeUnit.SECONDS);
        return count;
    }

    public void incrementDocConnections(Long docId) {
        redisTemplate.opsForValue().increment(DOC_CONNECTIONS_PREFIX + docId);
    }
    public void decrementDocConnections(Long docId){
        redisTemplate.opsForValue().increment(DOC_CONNECTIONS_PREFIX + docId,-1);
    }
}
