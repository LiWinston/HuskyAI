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

    private static final long INVITE_EXPIRE_TIME = 60 * 60; // 1小时

    /**
     * 将管理员的uuid和对应的token存入Redis，设置过期时间
     * @param token 生成的唯一标识符
     * @param uuid 管理员的uuid
     */
    public void addAdminToWaitingList(String token, String uuid) {
        redisTemplate.opsForValue().set(token, uuid, INVITE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 根据token从Redis中获取管理员的uuid
     * @param token 邀请链接中的唯一标识符
     * @return 管理员的uuid
     */
    public String getUuidByToken(String token) {
        return redisTemplate.opsForValue().get(token);
    }

    /**
     * 删除Redis中存储的token，确保一次性使用
     * @param token 邀请链接中的唯一标识符
     */
    public void removeToken(String token) {
        redisTemplate.delete(token);
    }

}
