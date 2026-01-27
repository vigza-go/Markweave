package com.vigza.markweave.infrastructure.service;

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

    public void addToBlacklist(String token){
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expiration, TimeUnit.SECONDS);
    }
    
    public boolean isBlacklisted(String token){
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
}
