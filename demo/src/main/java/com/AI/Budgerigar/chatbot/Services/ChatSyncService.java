package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.AIUtil.Message;

import java.util.List;

public interface ChatSyncService {

    // get历史传给前端显示，何尝不是一种同步捏
    List<Message> getHistory(String conversationId);

    void updateHistoryFromRedis(String conversationId, int numberOfEntries);

    void updateRedisFromMongo(String conversationId);

    void updateHistoryFromRedis(String conversationId);

    // int getConversationLength(String conversationId);

}
