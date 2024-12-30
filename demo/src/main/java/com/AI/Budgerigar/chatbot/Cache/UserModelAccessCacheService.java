package com.AI.Budgerigar.chatbot.Cache;

import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfig;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UserModelAccessCacheService {
    
    @Autowired
    private UserModelAccessConfigRepository userModelAccessConfigRepository;
    
    /**
     * 从缓存获取用户访问配置
     */
    @Cacheable(value = "user_model_access", key = "#userId", unless = "#result.isEmpty()")
    public Optional<UserModelAccessConfig> getAccessConfigFromCache(String userId) {
        log.info("从MongoDB获取用户模型访问配置: userId={}", userId);
        return userModelAccessConfigRepository.findById(userId);
    }
} 