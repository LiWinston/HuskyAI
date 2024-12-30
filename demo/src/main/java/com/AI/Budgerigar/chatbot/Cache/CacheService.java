package com.AI.Budgerigar.chatbot.Cache;

import com.AI.Budgerigar.chatbot.Cache.annotation.CacheEvictPattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 清除指定用户的会话列表缓存
     * @param uuid 用户UUID
     */
    @CacheEvictPattern(type = "user", paramName = "uuid")
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

    /**
     * 清除指定用户的存在性缓存
     * @param uuid 用户UUID
     */
    @CacheEvictPattern(type = "user", paramName = "uuid")
    public void clearUserExistsCache(String uuid) {
        String userExistsPattern = "user::exists:" + uuid;
        
        Set<String> userExistsKeys = redisTemplate.keys(userExistsPattern);
        if (userExistsKeys != null && !userExistsKeys.isEmpty()) {
            redisTemplate.delete(userExistsKeys);
            log.info("清除用户存在性缓存: pattern={}", userExistsPattern);
        }
    }

    /**
     * 清除指定分页大小的会话列表缓存
     * @param pageSize 分页大小
     */
    @CacheEvictPattern(type = "page", paramName = "pageSize")
    public void clearPageSizeConversationCaches(Integer pageSize) {
        String pattern = "conversations_page::*:page:*:" + pageSize;
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除指定分页大小的会话列表缓存: pattern={}, count={}", pattern, keys.size());
        }
    }

    /**
     * 清除所有指定类型的相关缓存
     * @param type 缓存类型
     * @param paramValue 参数值
     */
    public void clearTypeRelatedCaches(String type, Object paramValue) {
        Method[] methods = this.getClass().getDeclaredMethods();
        Arrays.stream(methods)
            .filter(method -> method.isAnnotationPresent(CacheEvictPattern.class))
            .filter(method -> {
                CacheEvictPattern annotation = method.getAnnotation(CacheEvictPattern.class);
                return annotation.type().equals(type);
            })
            .forEach(method -> {
                try {
                    method.invoke(this, paramValue);
                } catch (Exception e) {
                    log.error("清除缓存失败: method={}, type={}, paramValue={}", 
                            method.getName(), type, paramValue, e);
                }
            });
        log.info("清除所有{}类型的相关缓存完成: paramValue={}", type, paramValue);
    }

    /**
     * 清除指定用户的所有相关缓存
     * @param uuid 用户UUID
     */
    public void clearAllUserCaches(String uuid) {
        clearTypeRelatedCaches("user", uuid);
    }

    /**
     * 清除指定用户的模型访问配置缓存
     * @param userId 用户ID
     */
    @CacheEvictPattern(type = "user", paramName = "userId")
    public void clearUserModelAccessCache(String userId) {
        String pattern = "user_model_access::" + userId;
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除用户模型访问配置缓存: pattern={}", pattern);
        }
    }
} 