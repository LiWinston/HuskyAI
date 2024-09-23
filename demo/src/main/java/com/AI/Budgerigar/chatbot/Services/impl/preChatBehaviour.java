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

        // 添加用户输入到 Redis 对话历史
        chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(), prompt);

        // 从 Redis 中获取对话历史
        List<String[]> conversationHistory;
        try {
            conversationHistory = tokenLimiter.getAdaptiveConversationHistory(conversationId, 16000);
            log.info("自适应缩放到" + conversationHistory.size() + "条消息");
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
