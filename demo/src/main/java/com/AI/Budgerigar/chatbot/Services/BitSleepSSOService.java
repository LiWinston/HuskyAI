package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Config.BitSleepSSOConfig;
import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.Mapper.UserPwMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BitSleepSSOService {

    private final BitSleepSSOConfig ssoConfig;
    private final UserPwMapper userMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JsonNode getSSOUserInfo(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                ssoConfig.getUserInfoUrl(),
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to get SSO user info", e);
        }
        return null;
    }

    public UserPw findLocalUserBySSOId(String ssoId) {
        return userMapper.findBySSOId(ssoId);
    }

    public UserPw createLocalUser(JsonNode ssoUserInfo, boolean useAutoUsername, String username, String password) {
        String ssoId = ssoUserInfo.get("id").asText();
        String email = ssoUserInfo.get("email").asText();
        String name = ssoUserInfo.get("name").asText();

        // 检查用户名是否已存在
        if (useAutoUsername) {
            username = generateUniqueUsername(name);
        } else if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("Username already exists");
        }

        // 创建新用户
        UserPw user = new UserPw();
        user.setUsername(username);
        user.setPassword(password);  // 应该加密存储
        user.setEmail(email);
        user.setSsoId(ssoId);
        user.setUuid(UUID.randomUUID().toString());

        userMapper.insert(user);
        return user;
    }

    private String generateUniqueUsername(String baseName) {
        String username = baseName;
        int suffix = 1;
        while (userMapper.findByUsername(username) != null) {
            username = baseName + suffix++;
        }
        return username;
    }
} 