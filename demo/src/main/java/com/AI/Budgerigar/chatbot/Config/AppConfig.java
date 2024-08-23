package com.AI.Budgerigar.chatbot.Config;

import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.impl.BaiduChatServiceImpl;
import com.AI.Budgerigar.chatbot.Services.impl.DouBaoChatServiceImpl;
import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AppConfig {

    @Bean
    @Qualifier("openai")
    public ChatService openAIChatService() {
        try {
            return new OpenAIChatServiceImpl();
        } catch (Exception e) {
            // 可以选择记录日志或进行其他处理
            e.printStackTrace();
            return null;
        }
    }

    @Bean
    @Qualifier("doubao")
    public ChatService DouBaoChatService(Environment environment, ArkService arkService) {
        try {
            if (arkService == null) {
                return openAIChatService();
//                throw new IllegalStateException("ArkService is not available");
            }
            return new DouBaoChatServiceImpl(arkService);
        } catch (Exception e) {
            // 可以选择记录日志或进行其他处理
            e.printStackTrace();
            // 如果 ArkService 初始化失败，则返回 OpenAIChatService 实现类
//            return openAIChatService();
        }
        return null;
    }


    @Bean
    @Qualifier("baidu")
    public ChatService BaiduChatService() {
        try {
            return new BaiduChatServiceImpl();
        } catch (Exception e) {
            // 可以选择记录日志或进行其他处理
            e.printStackTrace();
            return null;
        }
    }
}
