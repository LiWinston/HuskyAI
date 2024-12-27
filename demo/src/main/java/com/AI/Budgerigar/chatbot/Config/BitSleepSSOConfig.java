package com.AI.Budgerigar.chatbot.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "bitsleep.sso")
public class BitSleepSSOConfig {
    private String serverUrl = "https://bitsleep.cn";
    private String userInfoUrl = serverUrl + "/sapi/user/info";
    private String loginUrl = serverUrl + "/sso/login";
} 