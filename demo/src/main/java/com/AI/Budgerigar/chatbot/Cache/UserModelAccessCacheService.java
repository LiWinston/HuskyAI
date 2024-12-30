package com.AI.Budgerigar.chatbot.Cache;

import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfig;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class UserModelAccessCacheService {
    
    @Autowired
    private UserModelAccessConfigRepository userModelAccessConfigRepository;
    
    @Autowired
    private CacheService cacheService;
    
    /**
     * 从缓存获取用户访问配置
     */
    @Cacheable(value = "user_model_access", key = "#userId", unless = "#result == null")
    public Optional<UserModelAccessConfig> getAccessConfigFromCache(String userId) {
        log.info("从MongoDB获取用户模型访问配置: userId={}", userId);
        return userModelAccessConfigRepository.findById(userId);
    }
    
    /**
     * 异步清除用户模型访问配置缓存
     * 返回CompletableFuture以便调用者可以选择是否等待完成
     */
    @Async
    public CompletableFuture<Boolean> asyncClearCache(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            int maxRetries = 3;
            for(int i = 0; i < maxRetries; i++) {
                try {
                    cacheService.clearUserModelAccessCache(userId);
                    log.info("成功清除用户{}的缓存", userId);
                    return true;
                } catch(Exception e) {
                    log.error("第{}次清除用户{}的缓存失败", i + 1, userId, e);
                    try {
                        Thread.sleep(100 * (i + 1));  // 退避策略
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            log.error("清除用户{}的缓存在{}次尝试后最终失败", userId, maxRetries);
            return false;
        });
    }
} 