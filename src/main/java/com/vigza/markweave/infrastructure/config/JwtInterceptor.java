package com.vigza.markweave.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.BusinessException;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.infrastructure.service.RedisService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    private void writeUnauthorizedResponse(HttpServletResponse response,String message) throws Exception{
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Result<?> result = Result.error(401, message);
    
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    public boolean preHandle(HttpServletRequest request ,HttpServletResponse response ,Object Handler) throws Exception{
        String authHeader = request.getHeader(jwtUtil.getHeader());
        if (authHeader == null || !authHeader.startsWith(jwtUtil.getPrefix())){
            writeUnauthorizedResponse(response,"未授权");
            return false;
        }
        String token = authHeader.substring(jwtUtil.getPrefix().length()).trim();
        if(!jwtUtil.validateToken(token)){
            writeUnauthorizedResponse(response,"token无效");
            return false;
        }
        if(redisService.isBlacklisted(token)){
            writeUnauthorizedResponse(response,"token已过期");
            return false;
        }
        return true;
    }
}