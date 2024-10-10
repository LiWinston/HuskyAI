package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Entity.Message;

import java.util.List;

public interface ShareService {

    // Generate share link.
    String generateShareLink(String uuid, String conversationId, List<Integer> messageIndexes);

    // Obtain conversation details based on the share code.
    List<Message> getSharedConversation(String shareCode);

    // Delete sharing history.
    void deleteShare(String shareCode);

}
