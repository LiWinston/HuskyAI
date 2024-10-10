package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.result.Result;

import java.util.List;

public interface ChatSyncService {

    // Get history and send it to the front end for display.
    List<Message> getHistory(String conversationId);

    void updateHistoryFromRedis(String conversationId, int numberOfEntries);

    void updateRedisFromMongo(String conversationId);

    void updateHistoryFromRedis(String conversationId);

    Result<?> deleteConversation(String uuid, String conversationId);

    // int getConversationLength(String conversationId);

}
