package com.vigza.markweave.infrastructure.config;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HandshakeInterceptorChain;

import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.infrastructure.persistence.entity.Collaboration;

@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Autowired
    private CollaborationService collaborationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHander,
            Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest req = servletRequest.getServletRequest();

            String token = req.getHeader("Authorization");
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserFromToken(token).getId();
                if (userId == null) {
                    return false;
                }
                attributes.put("userId", userId);
                attributes.put("token", token);
            } else
                return false;

        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }
}
