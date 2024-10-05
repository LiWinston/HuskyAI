package com.AI.Budgerigar.chatbot.Nosql;

import com.AI.Budgerigar.chatbot.Entity.Message;

import java.util.List;

public interface ChatMessagesMongoDAO {

    void updateHistoryById(String conversationId, List<Message> newMessages);

    int getConversationLengthById(String conversationId); // 新方法，用于获取对话长度

    List<Message> getConversationHistory(String conversationId);

    void replaceHistoryById(String conversationId, List<Message> newMessages);

    Boolean deleteConversationById(String conversationId);

}