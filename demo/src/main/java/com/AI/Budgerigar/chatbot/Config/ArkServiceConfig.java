package com.AI.Budgerigar.chatbot.Config;

import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArkServiceConfig {

    @Value("${volc.ak}")
    private String ak;

    @Value("${volc.sk}")
    private String sk;

    @Value("${volc.base-url}")
    private String baseUrl;

    @Value("${volc.region}")
    private String region;

    @Bean
    public ArkService arkService() {
        return ArkService.builder()
                .ak(ak)
                .sk(sk)
                .baseUrl(baseUrl)
                .region(region)
                .build();
    }
}
