package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import com.AI.Budgerigar.chatbot.Nosql.ShareDAO;
import com.AI.Budgerigar.chatbot.Nosql.ShareRecord;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.Services.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {

    private final ShareDAO shareDAO;

    private final ChatSyncService chatSyncService;

    public ShareServiceImpl(ShareDAO shareDAO, ChatSyncService chatSyncService) {
        this.shareDAO = shareDAO;
        this.chatSyncService = chatSyncService;
    }

    // 生成分享链接
    @Override
    public String generateShareLink(String uuid, String conversationId, List<Integer> messageIndexes) {
        String shareCode = UUID.randomUUID().toString(); // 生成唯一分享码
        // chatSyncService.updateHistoryFromRedis(conversationId); // 确保同步
        shareDAO.saveShare(shareCode, uuid, conversationId, messageIndexes);
        return shareCode;
    }

    // 根据分享码获取对话详情
    @Override
    public List<Message> getSharedConversation(String shareCode) {
        // 从 shareDAO 中根据 shareCode 获取对应的分享记录
        ShareRecord record = shareDAO.findByShareCode(shareCode);
        log.info("record:{}", record);
        if (record != null) {
            // 获取指定 conversationId 的完整消息列表
            List<Message> allMessages = chatSyncService.getHistory(record.getConversationId());

            // 根据记录中的索引筛选出被分享的消息
            List<Integer> messageIndexes = record.getMessageIndexes();

            return messageIndexes.stream()
                .filter(index -> index >= 0 && index < allMessages.size()) // 避免无效索引
                .map(allMessages::get) // 根据索引获取消息
                .collect(Collectors.toList());
        }
        log.error("Share record not found for share code: {}", shareCode);
        // 如果未找到记录，返回空列表
        return Collections.emptyList();
    }

    // 删除分享记录
    @Override
    public void deleteShare(String shareCode) {
        shareDAO.deleteByShareCode(shareCode);
    }

}
