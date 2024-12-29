package com.AI.Budgerigar.chatbot.Cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 清除指定用户的所有会话列表缓存
     * @param uuid 用户UUID
     */
    public void clearUserConversationCaches(String uuid) {
        String conversationsPattern = "conversations::" + uuid + "*";
        String conversationsPagePattern = "conversations_page::" + uuid + "*";
        
        Set<String> conversationsKeys = redisTemplate.keys(conversationsPattern);
        Set<String> conversationsPageKeys = redisTemplate.keys(conversationsPagePattern);
        
        if (conversationsKeys != null && !conversationsKeys.isEmpty()) {
            redisTemplate.delete(conversationsKeys);
            log.info("清除用户会话列表缓存: pattern={}, count={}", conversationsPattern, conversationsKeys.size());
        }
        
        if (conversationsPageKeys != null && !conversationsPageKeys.isEmpty()) {
            redisTemplate.delete(conversationsPageKeys);
            log.info("清除用户分页会话列表缓存: pattern={}, count={}", conversationsPagePattern, conversationsPageKeys.size());
        }
    }
} 