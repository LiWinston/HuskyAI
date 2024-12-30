package com.AI.Budgerigar.chatbot.Config;

import com.AI.Budgerigar.chatbot.Interceptor.PageContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private PageContextInterceptor pageContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageContextInterceptor)
                .addPathPatterns("/chat")  // 普通chat接口
                .addPathPatterns("/chat/stream")  // 流式chat接口
                .excludePathPatterns("/chat/**")  // 排除其他chat接口
                .order(1);  // 设置拦截器顺序
    }
} 