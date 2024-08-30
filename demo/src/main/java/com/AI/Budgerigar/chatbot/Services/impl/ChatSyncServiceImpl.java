package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAOImpl;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatSyncServiceImpl implements ChatSyncService {

    @Autowired
    private ChatMessagesMongoDAOImpl chatMessagesMongoDAO;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    // 更新历史记录，从 Redis 获取最新消息并更新到 MongoDB
    public void updateHistoryFromRedis(String conversationId, int numberOfEntries) {
        List<String[]> newEntries = chatMessagesRedisDAO.getConversationHistory(conversationId).stream()
                .skip(Math.max(0, chatMessagesRedisDAO.getConversationHistory(conversationId).size() - numberOfEntries))
                .toList();
        chatMessagesMongoDAO.updateHistoryById(conversationId, newEntries);
    }
}
