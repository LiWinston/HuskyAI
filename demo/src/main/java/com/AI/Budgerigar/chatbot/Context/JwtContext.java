package com.AI.Budgerigar.chatbot.Context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtContext {
    private static final ThreadLocal<String> currentUuid = new ThreadLocal<>();

    public static void setCurrentUuid(String uuid) {
        log.debug("设置当前UUID: {}", uuid);
        currentUuid.set(uuid);
    }

    public static String getCurrentUuid() {
        String uuid = currentUuid.get();
        log.debug("获取当前UUID: {}", uuid);
        return uuid;
    }

    public static void clear() {
        currentUuid.remove();
        log.debug("清除当前UUID");
    }
} 