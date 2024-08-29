package com.AI.Budgerigar.chatbot.Nosql;

public interface ChatMessagesMongoDAO {
    void updateHistoryById(String conversationId, int numberOfEntries);
    int getConversationLengthById(String conversationId); // 新方法，用于获取对话长度
}