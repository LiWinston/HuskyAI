package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Entity.Message;

import java.util.List;

public interface ShareService {

    // 生成分享链接
    String generateShareLink(String uuid, String conversationId, List<Integer> messageIndexes);

    // 根据分享码获取对话详情
    List<Message> getSharedConversation(String shareCode);

    // 删除分享记录
    void deleteShare(String shareCode);

}
