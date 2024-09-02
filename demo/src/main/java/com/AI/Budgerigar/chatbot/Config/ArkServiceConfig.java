package com.AI.Budgerigar.chatbot.Config;

import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

@Lazy
@Configuration
public class ArkServiceConfig {

    // default value useless, just avoid config not found leading to error launch
    // @Value("${volc.ak}")
    @Value("${volc.ak}")
    private String ak;

    @Value("${volc.sk}")
    private String sk;

    @Value("${volc.base-url}")
    private String baseUrl;

    @Value("${volc.region}")
    private String region;

    @Lazy
    @Bean
    // @ConditionalOnProperty(prefix = "volc", name = {"ak", "sk", "base-url", "region"})
    public ArkService arkService() {
        // Check if critical properties are provided
        if (StringUtils.hasText(ak) && StringUtils.hasText(sk) && StringUtils.hasText(baseUrl)
                && StringUtils.hasText(region)) {
            // All required properties are available, create and return ArkService
            // instance
            return ArkService.builder().ak(ak).sk(sk).baseUrl(baseUrl).region(region).build();
        }
        else {
            // Handle missing properties, e.g., log a warning or throw an exception
            throw new IllegalStateException("Missing required ArkService configuration properties");
            // Alternatively, you can return null or provide a default implementation
            // return null;
        }
    }

}
