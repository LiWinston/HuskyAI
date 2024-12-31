package com.AI.Budgerigar.chatbot.Cache;

import com.AI.Budgerigar.chatbot.Cache.annotation.CacheEvictPattern;
import com.AI.Budgerigar.chatbot.Context.PageContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class CacheService {
    
    private static final int PAGE_SIZE = 12;  // 与前端保持一致的页面大小
    
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

    /**
     * 清除指定范围内的用户对话页面缓存
     * @param userId 用户ID
     * @param fromPage 起始页(包含)
     * @param toPage 结束页(包含)
     */
    public void clearRangeUserConversationCaches(String userId, int fromPage, int toPage) {
        log.info("清除用户{}的对话缓存, 范围从第{}页到第{}页", userId, fromPage, toPage);
        for (int page = fromPage; page <= toPage; page++) {
            String pattern = String.format("conversations_page::%s:page:%d:%d", userId, page, PAGE_SIZE);
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("已清除第{}页缓存: pattern={}, count={}", page, pattern, keys.size());
            }
        }
    }

    /**
     * 异步清除受影响的对话页面缓存
     * 当一个对话被更新时,它会移动到第一页,需要清除从它原来所在页到第一页的所有缓存
     */
    @Async
    public CompletableFuture<Void> asyncClearAffectedConversationCaches(String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Integer sourcePage = PageContext.getCurrentPage();
                if (sourcePage == null || sourcePage <= 1) {
                    log.debug("源页面为空或为第一页,仅清除第一页缓存");
                    clearRangeUserConversationCaches(userId, 1, 1);
                    return;
                }
                
                log.info("清除从第{}页到第1页的所有缓存", sourcePage);
                clearRangeUserConversationCaches(userId, 1, sourcePage);
            } catch (Exception e) {
                log.error("异步清除缓存失败: userId={}", userId, e);
            }
        });
    }
}