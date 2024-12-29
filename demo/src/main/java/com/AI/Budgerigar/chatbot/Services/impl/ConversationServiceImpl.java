package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Services.ConversationService;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    public boolean checkConversationExists(String uuid, String conversationId) {
        return conversationMapper.checkConversationExistsByUuid(uuid, conversationId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"conversations", "conversations_page"}, key = "#uuid + '*'", allEntries = false)
    public Result<?> createConversation(String uuid, String conversationId) {
        try {
            int result = conversationMapper.createConversationForUuid(uuid, conversationId);
            if (result > 0) {
                log.info("Successfully created conversation for UUID: {}, conversation ID: {}", uuid, conversationId);
                return Result.success("Conversation created successfully");
            } else {
                log.error("Failed to create conversation for UUID: {}, conversation ID: {}", uuid, conversationId);
                return Result.error("Failed to create conversation");
            }
        } catch (Exception e) {
            log.error("Error occurred while creating conversation: {}", e.getMessage(), e);
            return Result.error("Error creating conversation: " + e.getMessage());
        }
    }
} 