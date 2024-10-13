package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Entity.Message;
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

    // Generate share link.
    @Override
    public String generateShareLink(String uuid, String conversationId, List<Integer> messageIndexes) {
        String shareCode = UUID.randomUUID().toString(); // Generate a unique sharing
                                                         // code.
        // chatSyncService.updateHistoryFromRedis(conversationId); // Ensure that the
        // conversation history is up to date.
        shareDAO.saveShare(shareCode, uuid, conversationId, messageIndexes);
        return shareCode;
    }

    // Get conversation details based on the shared code.
    @Override
    public List<Message> getSharedConversation(String shareCode) {
        // Retrieve the corresponding sharing record from shareDAO based on shareCode.
        ShareRecord record = shareDAO.findByShareCode(shareCode);
        log.info("record:{}", record);
        if (record != null) {
            // Get the complete message list of the specified conversationId.
            List<Message> allMessages = chatSyncService.getHistory(record.getConversationId());

            // Filter out the shared messages based on the index in the records.
            List<Integer> messageIndexes = record.getMessageIndexes();

            return messageIndexes.stream()
                .filter(index -> index >= 0 && index < allMessages.size()) // Avoid
                                                                           // invalid
                                                                           // indexes.
                .map(allMessages::get) // Get message by index.
                .collect(Collectors.toList());
        }
        log.error("Share record not found for share code: {}", shareCode);
        // Return an empty list if no record is found.
        return Collections.emptyList();
    }

    // Delete sharing record.
    @Override
    public void deleteShare(String shareCode) {
        shareDAO.deleteByShareCode(shareCode);
    }

}
