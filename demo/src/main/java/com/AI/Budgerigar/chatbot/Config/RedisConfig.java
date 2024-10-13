package com.AI.Budgerigar.chatbot.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("Start creating RedisTemplate...");
        // RedisTemplate redisTemplate = new RedisTemplate();
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // Set the connection factory object for Redis.
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // Set the serializer for the Redis key (default is
        // JdkSerializationRedisSerializer).
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

}
