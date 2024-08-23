package com.AI.Budgerigar.chatbot.Services;

import java.util.logging.Logger;

public interface ChatService {
    Logger logger = Logger.getLogger(ChatService.class.getName());
    String chat(String prompt) throws Exception;

    // 默认方法来记录信息
    default void logInfo(String message) {
        String className = getCallingClassName();
        logger.info(className + " : " + message.substring(0, Math.min(40, message.length())));
    }

    // 默认方法来记录错误
    default void logError(String message, Throwable throwable) {
        String className = getCallingClassName();
        logger.severe(className + " : " + message);
        logger.throwing(className, "logError", throwable);
    }

    default String getCallingClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 查找调用当前方法的类
        for (int i = 1; i < stackTrace.length; i++) {
            if (stackTrace[i].getMethodName().equals("chat")) {
                return stackTrace[i].getClassName();
            }
        }
        return "Unknown";
    }
}
