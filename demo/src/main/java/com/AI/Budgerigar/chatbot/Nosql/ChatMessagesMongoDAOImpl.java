package com.AI.Budgerigar.chatbot.Nosql;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.ChatConversation;
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
        try {
            // 使用聚合框架来计算消息数量
            MatchOperation match = Aggregation.match(Criteria.where("_id").is(conversationId));
            ProjectionOperation project = Aggregation.project().and("messages").size().as("messageCount");
            Aggregation aggregation = Aggregation.newAggregation(match, project);

            AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, "chatConversation", Document.class);
            Document document = result.getUniqueMappedResult();

            int messageCount = (document != null) ? document.getInteger("messageCount", 0) : 0;

            log.info("Conversation ID: " + conversationId + ", Message count: " + messageCount);
            return messageCount;

        } catch (Exception e) {
            log.error("Failed to get conversation length for conversation ID: {}", conversationId, e);
            return 0; // 出现异常时返回0
        }
    }
    private List<String[]> getNewEntriesFromRedis(String conversationId, int numberOfEntries) {
        //返回最新的numberOfEntries条消息
        return chatMessagesRedisDAO.getConversationHistory(conversationId).stream()
                .skip(Math.max(0, chatMessagesRedisDAO.getConversationHistory(conversationId).size() - numberOfEntries))
                .toList();
    }
}