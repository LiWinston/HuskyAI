package com.AI.Budgerigar.chatbot.Services;

public interface ChatSyncService {

    void updateHistoryFromRedis(String conversationId, int numberOfEntries);

//    int getConversationLength(String conversationId);
}
