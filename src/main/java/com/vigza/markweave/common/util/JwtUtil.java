package com.vigza.markweave.common.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vigza.markweave.infrastructure.persistence.entity.User;

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

    public String generateToken(User user){
        Map<String,Object> payload = new HashMap<>();
        payload.put("user",user);
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

    public User getUserFromToken(String token){
        JWT jwt = JWTUtil.parseToken(token);
        return (User) jwt.getPayload("user");
    }

    public String generateInvitationToken(Map<String, Object> payload) {
        payload.put("iat", System.currentTimeMillis());
        return JWTUtil.createToken(payload, secret.getBytes());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getInvitationPayload(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            if (!jwt.setKey(secret.getBytes()).verify()) {
                return null;
            }
            if (jwt.validate(0)) {
                return null;
            }
            Object type = jwt.getPayload("type");
            if (!"invitation".equals(type)) {
                return null;
            }
            return (Map<String, Object>) jwt.getPayloads();
        } catch (Exception e) {
            return null;
        }
    }

}
