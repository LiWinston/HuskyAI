package com.AI.Budgerigar.chatbot.Cache.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvictPattern {
    /**
     * 缓存类型，如 user, conversation 等
     */
    String type();

    /**
     * 参数名称，如 uuid, pageSize 等
     */
    String paramName();
} 