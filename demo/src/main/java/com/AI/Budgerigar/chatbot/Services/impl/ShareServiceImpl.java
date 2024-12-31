package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.Nosql.ShareDAO;
import com.AI.Budgerigar.chatbot.Nosql.ShareRecord;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.Services.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {

    private final ShareDAO shareDAO;
    private final ChatSyncService chatSyncService;

    public ShareServiceImpl(ShareDAO shareDAO, ChatSyncService chatSyncService) {
        this.shareDAO = shareDAO;
        this.chatSyncService = chatSyncService;
    }

    // Generate share link with expiration time
    @Override
    public String generateShareLink(String uuid, String conversationId, List<Integer> messageIndexes, int expirationHours) {
        String shareCode = UUID.randomUUID().toString();
        
        // Calculate expiration time
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expirationHours);
        Date expireAt = calendar.getTime();
        
        shareDAO.saveShare(shareCode, uuid, conversationId, messageIndexes, expireAt);
        return shareCode;
    }

    // Get conversation details based on the shared code
    @Override
    public List<Message> getSharedConversation(String shareCode) {
        ShareRecord record = shareDAO.findByShareCode(shareCode);
        log.info("record:{}", record);
        
        if (record != null) {
            // Check if share has expired
            if (record.getExpireAt() != null && record.getExpireAt().before(new Date())) {
                log.error("Share has expired: {}", shareCode);
                return Collections.emptyList();
            }
            
            List<Message> allMessages = chatSyncService.getHistory(record.getConversationId());
            List<Integer> messageIndexes = record.getMessageIndexes();

            return messageIndexes.stream()
                .filter(index -> index >= 0 && index < allMessages.size())
                .map(allMessages::get)
                .collect(java.util.stream.Collectors.toList());
        }
        
        log.error("Share record not found for share code: {}", shareCode);
        return Collections.emptyList();
    }

    // Delete sharing record
    @Override
    public void deleteShare(String shareCode) {
        shareDAO.deleteByShareCode(shareCode);
    }

    // Get all shares by user UUID
    @Override
    public List<ShareRecord> getUserShares(String uuid) {
        return shareDAO.findAllByUuid(uuid);
    }

    // Update share expiration time
    @Override
    public void updateShareExpiration(String shareCode, int newExpirationHours) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, newExpirationHours);
        Date newExpireAt = calendar.getTime();
        
        shareDAO.updateExpireAt(shareCode, newExpireAt);
    }

    // Delete all shares for a conversation
    @Override
    public void deleteSharesByConversation(String conversationId) {
        shareDAO.deleteByConversationId(conversationId);
    }
}
