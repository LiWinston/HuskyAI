package com.AI.Budgerigar.chatbot.Nosql;

import java.util.List;

public interface ChatMessagesMongoDAO {


    void updateHistoryById(String conversationId, List<String[]> newEntries);

    int getConversationLengthById(String conversationId); // 新方法，用于获取对话长度
}