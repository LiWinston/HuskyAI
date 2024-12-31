package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.Nosql.ShareRecord;

import java.util.Date;
import java.util.List;

public interface ShareService {

    // Generate share link with expiration time
    String generateShareLink(String uuid, String conversationId, List<Integer> messageIndexes, int expirationHours);

    // Obtain conversation details based on the share code
    List<Message> getSharedConversation(String shareCode);

    // Delete sharing history
    void deleteShare(String shareCode);

    // Get all shares by user UUID
    List<ShareRecord> getUserShares(String uuid);

    // Update share expiration time
    void updateShareExpiration(String shareCode, int newExpirationHours);

    // Delete all shares for a conversation
    void deleteSharesByConversation(String conversationId);
}
