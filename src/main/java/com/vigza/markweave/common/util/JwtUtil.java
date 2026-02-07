package com.vigza.markweave.common.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.vigza.markweave.infrastructure.persistence.entity.User;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
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
        long now  = System.currentTimeMillis() / 1000;
        payload.put(JWTPayload.ISSUED_AT,now);
        payload.put(JWTPayload.EXPIRES_AT,now + expiration );
        return JWTUtil.createToken(payload,secret.getBytes());
    }

    public String generateInvitaionToken(Long docId,Integer permission,Integer expTime){
        Map<String,Object> payload = new HashMap<>();
        payload.put("docId",docId);
        payload.put("permission",permission);
        long now = System.currentTimeMillis() / 1000;
        payload.put(JWTPayload.ISSUED_AT,now);
        payload.put(JWTPayload.EXPIRES_AT,now + expTime);
        return JWTUtil.createToken(payload,secret.getBytes());
    }

    public boolean validateToken(String token){
        if(token.startsWith(prefix)){
            token = token.substring(prefix.length()).trim();
        }
        try{
            JWT jwt = JWTUtil.parseToken(token);
            return jwt.setKey(secret.getBytes()).verify() && jwt.validate(0);
        }catch (Exception e){
            return false;
        }
    }

    public User getUserFromToken(String token){
        if(token.startsWith(prefix)){
            token = token.substring(prefix.length()).trim();
        }
        JWT jwt = JWTUtil.parseToken(token);
        Object userObject =  jwt.getPayload("user");
        return BeanUtil.toBean(userObject,User.class); 
    }

    public Long getDocIdFromInvToken(String invToken){
        if(invToken.startsWith(prefix)){
            invToken = invToken.substring(prefix.length()).trim();
        }
        JWT jwt = JWTUtil.parseToken(invToken);
        Object docIdObject =  jwt.getPayload("docId");
        return BeanUtil.toBean(docIdObject, Long.class);
    }    

    public Integer getPermissionFromInvToken(String invToken){
        if(invToken.startsWith(prefix)){
            invToken = invToken.substring(prefix.length()).trim();
        }
        JWT jwt = JWTUtil.parseToken(invToken);
        Object pObject = jwt.getPayload("permission");
        return BeanUtil.toBean(pObject,Integer.class);
    }
}
