package com.AI.Budgerigar.chatbot.Config;

import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.impl.BaiduChatServiceImpl;
import com.AI.Budgerigar.chatbot.Services.impl.DouBaoChatServiceImpl;
import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Configuration
@Slf4j
public class AppConfig {

    @Bean
    public ExecutorService executorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Use BCrypt for password encryption.
    }

    @Bean
    public ChatService.TokenLimitType tokenLimitType(Environment environment) {
        String tokenLimitTypeStr = environment.getProperty("chatbot.tokenLimitType");

        if (tokenLimitTypeStr == null || tokenLimitTypeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("The property 'chatbot.tokenLimitType' is not set or is empty.");
        }

        // Normalize the string to lower case to allow case-insensitive comparison
        String normalizedValue = tokenLimitTypeStr.trim().toLowerCase();

        // Define a map for fuzzy matching
        Map<String, ChatService.TokenLimitType> tokenLimitTypeMap = new HashMap<>();
        tokenLimitTypeMap.put("adaptive", ChatService.TokenLimitType.Adaptive);
        tokenLimitTypeMap.put("adapt", ChatService.TokenLimitType.Adaptive);
        tokenLimitTypeMap.put("adpt", ChatService.TokenLimitType.Adaptive);
        tokenLimitTypeMap.put("fixed", ChatService.TokenLimitType.Fixed);
        tokenLimitTypeMap.put("fix", ChatService.TokenLimitType.Fixed);
        tokenLimitTypeMap.put("fxd", ChatService.TokenLimitType.Fixed);

        // Perform fuzzy matching using the map
        if (tokenLimitTypeMap.containsKey(normalizedValue)) {
            return tokenLimitTypeMap.get(normalizedValue);
        }

        // As a fallback, use EnumUtils to perform a case-insensitive exact match
        ChatService.TokenLimitType type = EnumUtils.getEnumIgnoreCase(ChatService.TokenLimitType.class,
                tokenLimitTypeStr);
        if (type != null) {
            return type;
        }
        log.warn("Invalid token limit type: {}. Using default value: 'Adaptive'", tokenLimitTypeStr);
        return ChatService.TokenLimitType.Adaptive;
    }

    @Bean
    @Qualifier("chatServices")
    protected ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    @Qualifier("openai")
    public ChatService openAIChatService() {
        try {
            return new OpenAIChatServiceImpl();
        }
        catch (Exception e) {
            // May choose to log or perform other processing.
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
                // throw new IllegalStateException("ArkService is not available");
            }
            return new DouBaoChatServiceImpl(arkService);
        }
        catch (Exception e) {
            // May choose to log or perform other processing.
            e.printStackTrace();
            // If "ArkService" fails to initialize，return "OpenAIChatService" class.
            // return openAIChatService();
        }
        return null;
    }

    @Bean
    @Qualifier("baidu")
    public ChatService BaiduChatService() {
        try {
            return new BaiduChatServiceImpl();
        }
        catch (Exception e) {
            // May choose to log or perform other processing.
            e.printStackTrace();
            return null;
        }
    }

}
