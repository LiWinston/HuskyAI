package com.AI.Budgerigar.chatbot.Interceptor;

import com.AI.Budgerigar.chatbot.Context.UserContext;
import com.AI.Budgerigar.chatbot.security.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        String userUUID = request.getHeader("X-User-UUID");
        
        // 如果是登录或注册请求,不需要验证
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/user/login") || requestURI.startsWith("/user/register")) {
            return true;
        }
        
        // 其他请求需要验证token和userUUID
        if (token == null || !token.startsWith("Bearer ") || userUUID == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("缺少token或userUUID");
            return false;
        }
        
        token = token.substring(7);
        try {
            if (!jwtTokenUtil.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                log.warn("token验证失败");
                return false;
            }
            
            String tokenUUID = jwtTokenUtil.getUuidFromToken(token);
            if (!tokenUUID.equals(userUUID)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                log.warn("token中的UUID与请求头中的UUID不匹配: token={}, header={}", tokenUUID, userUUID);
                return false;
            }
            
            UserContext.setCurrentUuid(tokenUUID);
            log.debug("JWT认证成功, 用户UUID: {}", tokenUUID);
            return true;
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.error("JWT认证失败", e);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
        log.debug("清除用户上下文");
    }
} 