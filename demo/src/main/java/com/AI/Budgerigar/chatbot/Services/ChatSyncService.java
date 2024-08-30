package com.AI.Budgerigar.chatbot.Services;

public interface ChatSyncService {

    void updateHistoryFromRedis(String conversationId, int numberOfEntries);

    void updateRedisFromMongo(String conversationId);

    void updateHistoryFromRedis(String conversationId);

//    int getConversationLength(String conversationId);
}
