package com.AI.Budgerigar.chatbot.Config;

import com.AI.Budgerigar.chatbot.Interceptor.JwtAuthInterceptor;
import com.AI.Budgerigar.chatbot.Interceptor.PageContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    private PageContextInterceptor pageContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加JWT认证拦截器,优先级最高
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/user/login", 
                    "/user/register/**", 
                    "/health",
                    "/chat/share/{shareCode}"  // 只排除查看分享内容的接口
                )
                .order(1);

        // 页面上下文拦截器
        registry.addInterceptor(pageContextInterceptor)
                .addPathPatterns("/chat", "/chat/stream")
                .order(2);
    }
} 