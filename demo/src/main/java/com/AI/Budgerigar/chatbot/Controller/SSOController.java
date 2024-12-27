package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.Services.BitSleepSSOService;
import com.AI.Budgerigar.chatbot.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sso")
@RequiredArgsConstructor
public class SSOController {

    private final BitSleepSSOService ssoService;

    @PostMapping("/callback")
    public Result<?> handleCallback(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null) {
            return Result.error("Token is required");
        }

        try {
            // 获取SSO用户信息
            JsonNode ssoUserInfo = ssoService.getSSOUserInfo(token);
            if (ssoUserInfo == null) {
                return Result.error("Failed to get SSO user info");
            }

            // 查找是否已存在关联的本地用户
            UserPw localUser = ssoService.findLocalUserBySSOId(ssoUserInfo.get("id").asText());
            if (localUser != null) {
                // 已存在关联用户，返回登录信息
                Map<String, String> data = new HashMap<>();
                data.put("token", localUser.getUuid());  // 使用UUID作为token
                data.put("uuid", localUser.getUuid());
                return Result.success(data, "Login successful");
            }

            // 不存在关联用户，返回需要注册的状态
            return Result.success(ssoUserInfo, "Registration required").setCode(2);
        } catch (Exception e) {
            log.error("SSO callback error", e);
            return Result.error("SSO callback failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<?> handleRegister(@RequestBody Map<String, Object> body) {
        String token = (String) body.get("token");
        Boolean useAutoUsername = (Boolean) body.get("useAutoUsername");
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        try {
            // 获取SSO用户信息
            JsonNode ssoUserInfo = ssoService.getSSOUserInfo(token);
            if (ssoUserInfo == null) {
                return Result.error("Failed to get SSO user info");
            }

            // 创建本地用户
            UserPw user = ssoService.createLocalUser(ssoUserInfo, 
                useAutoUsername != null && useAutoUsername, 
                username, 
                password);

            // 返回登录信息
            Map<String, String> data = new HashMap<>();
            data.put("token", user.getUuid());  // 使用UUID作为token
            data.put("uuid", user.getUuid());
            return Result.success(data, "Registration successful");
        } catch (Exception e) {
            log.error("SSO registration error", e);
            return Result.error("SSO registration failed: " + e.getMessage());
        }
    }
} 