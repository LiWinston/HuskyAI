package com.AI.Budgerigar.chatbot.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("初始化缓存管理器...");
        
        // 创建ObjectMapper并配置
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        // 添加Java 8日期时间模块和Optional支持
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());  // 添加JDK8模块，支持Optional
        log.info("配置ObjectMapper完成，已添加JavaTimeModule和Jdk8Module支持");
        
        // 创建序列化器
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // 默认过期时间1小时
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer))
                .computePrefixWith(cacheName -> cacheName + "::");  // 添加缓存前缀，确保不同缓存空间隔离

        // 特定缓存配置
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        
        // 用户存在性缓存配置 - 24小时过期
        configMap.put("user", defaultConfig.entryTtl(Duration.ZERO)); // 永不过期
        log.info("配置user缓存: 过期时间=24小时");
        
        // 会话列表缓存配置 - 持久化缓存
        configMap.put("conversations", defaultConfig.entryTtl(Duration.ZERO));  // 永不过期
        configMap.put("conversations_page", defaultConfig.entryTtl(Duration.ZERO));  // 永不过期
        log.info("配置conversations和conversations_page缓存: 永不过期");

        // 用户模型访问配置缓存 - 1小时过期
        configMap.put("user_model_access", defaultConfig.entryTtl(Duration.ofDays(31)));
        log.info("配置user_model_access缓存: 过期时间=1小时");

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .build();
                
        log.info("缓存管理器初始化完成");
        return cacheManager;
    }
} 