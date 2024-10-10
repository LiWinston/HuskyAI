package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class preChatBehaviour {

    @Autowired
    private ChatSyncService chatSyncService;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Autowired
    private TokenLimiter tokenLimiter;

    @Autowired
    DateTimeFormatter dateTimeFormatter;

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @Transactional
    public List<String[]> getHistoryPreChat(Object caller, String prompt, String conversationId) {
        chatSyncService.updateRedisFromMongo(conversationId);

        chatMessagesRedisDAO.maintainMessageHistory(conversationId);

        // Add user input to Redis conversation history.
        chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(), prompt);

        // Retrieve conversation history from Redis.
        List<String[]> conversationHistory;
        try {
            conversationHistory = tokenLimiter.getAdaptiveConversationHistory(conversationId, 16000);
            log.info("Adaptive zoom to" + conversationHistory.size() + "message(s).");
            // for (String[] entry : conversationHistory) {
            // log.info("{} : {}", entry[0], entry[2].substring(0, Math.min(20,
            // entry[2].length())));
            // }
            return conversationHistory;
        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", TokenLimiter.class.getName(), e.getMessage(), e);
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    "Query failed. Please try again.");
            throw new RuntimeException("Error processing chat request", e);
        }
    }

}
