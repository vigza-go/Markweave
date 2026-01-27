package com.vigza.markweave.common.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import lombok.Data;

@Component
@Data
public class JwtUtil {
    private String secret = "vigza-2026";

    private Long expiration = 604800L;

    private String header = "Authorization";

    private String prefix = "Bearer";

    public String generateToken(Long userId,String account){
        Map<String,Object> payload = new HashMap<>();
        payload.put("userId",userId);
        payload.put("account",account);
        payload.put("iat",System.currentTimeMillis());
        payload.put("exp",System.currentTimeMillis() + expiration);
        return JWTUtil.createToken(payload,secret.getBytes());
    }

    public boolean validateToken(String token){
        try{
            JWT jwt = JWTUtil.parseToken(token);
            return jwt.setKey(secret.getBytes()).verify() && jwt.validate(0);
        }catch (Exception e){
            return false;
        }
    }

    public Long getUserIdFromToken(String token){
        JWT jwt = JWTUtil.parseToken(token);
        return (Long) jwt.getPayload("userId");
    }

    public String getAccountFromToken(String token){
        JWT jwt = JWTUtil.parseToken(token);
        return (String) jwt.getPayload("account");
    }


}
