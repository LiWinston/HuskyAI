package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.result.Result;

public interface ConversationService {
    
    /**
     * 创建新的会话
     * @param uuid 用户UUID
     * @param conversationId 会话ID
     * @return 创建结果
     */
    Result<?> createConversation(String uuid, String conversationId);

    /**
     * 检查会话是否存在
     * @param uuid 用户UUID
     * @param conversationId 会话ID
     * @return 是否存在
     */
    boolean checkConversationExists(String uuid, String conversationId);
} 