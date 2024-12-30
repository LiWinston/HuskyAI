package com.AI.Budgerigar.chatbot.Interceptor;

import com.AI.Budgerigar.chatbot.Context.PageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class PageContextInterceptor implements HandlerInterceptor {
    
    private static final String CONVERSATION_PAGE_HEADER = "X-Conversation-Page";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        
        try {
            String pageHeader = request.getHeader(CONVERSATION_PAGE_HEADER);
            if (pageHeader != null) {
                Integer page = Integer.parseInt(pageHeader);
                PageContext.setCurrentPage(page);
                log.debug("从请求头获取对话页面: {}", page);
            }
        } catch (NumberFormatException e) {
            log.warn("解析对话页面请求头失败", e);
        }
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        PageContext.clear();
    }
} 