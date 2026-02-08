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
        String token = null;
        String clientId = null;
        
        
        if ((token == null || token.isEmpty()) && request.getURI() != null) {
            String query = request.getURI().getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        if ("token".equals(keyValue[0])) {
                            token = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                        } else if ("clientId".equals(keyValue[0])) {
                            clientId = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                        }
                    }
                }
            }
        }
        
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserFromToken(token).getId();
            if (userId == null) {
                return false;
            }
            attributes.put("userId", userId);
            attributes.put("token", token);
            attributes.put("clientId", clientId);
            return true;
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }
}
