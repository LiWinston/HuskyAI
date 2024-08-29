package com.AI.Budgerigar.chatbot.Nosql;


import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.ChatConversation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.logging.Logger;

@Repository
public class ChatMessagesMongoDAOImpl implements ChatMessagesMongoDAO {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Override
    public void updateHistoryById(String conversationId, int numberOfEntries) {
        ChatConversation chatConversation = mongoTemplate.findById(conversationId, ChatConversation.class);
        List<String[]> newEntries = getNewEntriesFromRedis(conversationId, numberOfEntries);
        if (chatConversation != null) {
            chatConversation.addMessage(newEntries);
            mongoTemplate.save(chatConversation);
        } else {
            ChatConversation newChatConversation = new ChatConversation();
            newChatConversation.setMessages(newEntries);
            newChatConversation.setConversationId(conversationId);
            mongoTemplate.save(newChatConversation);
        }
    }

    @Override
    public int getConversationLengthById(String conversationId) {
        ChatConversation chatConversation = mongoTemplate.findById(conversationId, ChatConversation.class);
        Logger logger = Logger.getLogger(ChatMessagesMongoDAOImpl.class.getName());
        if (chatConversation != null) {
            logger.info(chatConversation.toString());
        }else {
            logger.info("getConversationLengthById -- result null");
        }
        return chatConversation != null ? chatConversation.getMessages().size() : 0;
    }

    private List<String[]> getNewEntriesFromRedis(String conversationId, int numberOfEntries) {
        //返回最新的numberOfEntries条消息
        return chatMessagesRedisDAO.getConversationHistory(conversationId).stream()
                .skip(Math.max(0, chatMessagesRedisDAO.getConversationHistory(conversationId).size() - numberOfEntries))
                .toList();
    }
}