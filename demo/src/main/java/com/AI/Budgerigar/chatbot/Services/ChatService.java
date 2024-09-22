package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.result.Result;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public interface ChatService {

    enum TokenLimitType {

        Adaptive, Fixed

    }

    Logger logger = Logger.getLogger(ChatService.class.getName());

//    List<String[]> getHistoryPreChat(String prompt, String conversationId) throws Exception;

    Result<String> chat(String prompt, String conversationId) throws Exception;

    // 默认方法来记录信息
    // default void logInfo(String message) {
    // String className = getCallingClassName();
    // logger.info(className + " : " + message.substring(0, Math.min(40,
    // message.length())));
    // }

    //
    default void logInfo(String message) {
        String className = getCallingClassName();
        Logger logger = Logger.getLogger(className);

        // Format the message to split into lines if it exceeds 200 characters
        StringBuilder formattedMessage = new StringBuilder();
        int maxLineLength = 200;
        int start = 0;

        // Loop through the message and break into lines
        while (start < message.length()) {
            int end = Math.min(start + maxLineLength, message.length());
            formattedMessage.append(message, start, end);

            // If not at the end of the message, add a newline for the next chunk
            if (end < message.length()) {
                formattedMessage.append("\n");
            }

            start = end;
        }

        // Log the formatted message
        logger.info(className + " : " + formattedMessage.toString());
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

    // void setConversationId(String conversationId);
    // String getConversationId();

}
