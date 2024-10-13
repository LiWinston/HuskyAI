package com.AI.Budgerigar.chatbot.Cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class AdminWaitingListRedisDAO {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final long INVITE_EXPIRE_TIME = 60 * 60; // 1 hour

    /**
     * Store the administrator's UUID and corresponding token in Redis and set an
     * expiration time.
     * @param token generated unique identifier.
     * @param uuid admin's uuid.
     */
    public void addAdminToWaitingList(String token, String uuid) {
        redisTemplate.opsForValue().set(token, uuid, INVITE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * Get the administrator's UUID from Redis according to the token.
     * @param token unique identifier in the invitation link.
     * @return admin's uuid.
     */
    public String getUuidByToken(String token) {
        return redisTemplate.opsForValue().get(token);
    }

    /**
     * Delete the token stored in Redis to ensure one-time use.
     * @param token unique identifier in the invitation link.
     */
    public void removeToken(String token) {
        redisTemplate.delete(token);
    }

}
