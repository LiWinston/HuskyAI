package com.AI.Budgerigar.chatbot.Nosql;

import com.AI.Budgerigar.chatbot.DTO.ChatConversationDTO;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@Slf4j
public class ChatMessagesMongoDAOImpl implements ChatMessagesMongoDAO {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void updateHistoryById(String conversationId, List<String[]> newEntries) {
        ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);
        if (chatConversationDTO != null) {
            chatConversationDTO.addMessage(newEntries);
            mongoTemplate.save(chatConversationDTO);
        } else {
            ChatConversationDTO newChatConversationDTO = new ChatConversationDTO();
            newChatConversationDTO.setMessages(newEntries);
            newChatConversationDTO.setConversationId(conversationId);
            mongoTemplate.save(newChatConversationDTO);
        }
    }

    @Override
    public int getConversationLengthById(String conversationId) {
        try {
            // 使用聚合框架来计算消息数量
            MatchOperation match = Aggregation.match(Criteria.where("_id").is(conversationId));
            ProjectionOperation project = Aggregation.project().and("messages").size().as("chat_conversations");
            Aggregation aggregation = Aggregation.newAggregation(match, project);

            AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, "chat_conversations", Document.class);
            Document document = result.getUniqueMappedResult();

            if (document != null) {
                int messageCount = document.getInteger("messageCount", 0);
                log.info("Conversation ID: " + conversationId + ", Message count: " + messageCount);
                return messageCount;
            } else {
                log.warn("No document found for Conversation ID: " + conversationId);
                return 0;
            }

        } catch (Exception e) {
            log.error("Failed to get conversation length for conversation ID: {}", conversationId, e);
            return 0; // 出现异常时返回0
        }
    }

}