package com.AI.Budgerigar.chatbot.Nosql;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
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
import java.util.stream.Collectors;

@Repository
@Slf4j
public class ChatMessagesMongoDAOImpl implements ChatMessagesMongoDAO {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void updateHistoryById(String conversationId, List<Message> newMessages) {
        List<String[]> messageArrays = newMessages.stream()
                .map(this::convertToArray)
                .collect(Collectors.toList());

        ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);
        if (chatConversationDTO != null) {
            chatConversationDTO.addStringMessages(messageArrays); // 使用字符串数组
            mongoTemplate.save(chatConversationDTO);
        } else {
            ChatConversationDTO newChatConversationDTO = new ChatConversationDTO();
            newChatConversationDTO.setMessages(messageArrays);
            newChatConversationDTO.setConversationId(conversationId);
            mongoTemplate.save(newChatConversationDTO);
        }
    }

    @Override
    public int getConversationLengthById(String conversationId) {
        try {
            MatchOperation match = Aggregation.match(Criteria.where("_id").is(conversationId));
            ProjectionOperation project = Aggregation.project().and("messages").size().as("messageCount");
            Aggregation aggregation = Aggregation.newAggregation(match, project);

            AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, "chat_conversations", Document.class);
            Document document = result.getUniqueMappedResult();

            if (document != null) {
                return document.getInteger("messageCount", 0);
            } else {
                return 0;
            }

        } catch (Exception e) {
            log.error("Failed to get conversation length for conversation ID: {}", conversationId, e);
            return 0;
        }
    }

    @Override
    public List<Message> getConversationHistory(String conversationId) {
        ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);
        if (chatConversationDTO != null) {
            List<String[]> messagesList = chatConversationDTO.getMessages();

            // Check if messagesList is actually a List<String[]> and not List<String[][]>
            if (messagesList != null && !messagesList.isEmpty() && messagesList.get(0) != null) {
                return messagesList.stream()
                        .map(this::convertToMessage)
                        .collect(Collectors.toList());
            } else {
                log.error("Invalid message structure for conversation ID: " + conversationId);
                return List.of();
            }
        } else {
            return List.of();
        }
    }

    // 将 String[] 转换为 Message 对象的方法
    private Message convertToMessage(String[] messageArray) {
        if (messageArray.length == 3) {
            return new Message(messageArray[0], messageArray[1], messageArray[2]);
        } else {
            throw new IllegalArgumentException("Invalid message format. Expected [role, timestamp, content]");
        }
    }

    @Override
    public void replaceHistoryById(String conversationId, List<Message> newMessages) {
        // 将 Message 对象列表转换为 String[] 的列表
        List<String[]> messageArrays = newMessages.stream()
                .map(this::convertToArray)
                .collect(Collectors.toList());

        // 查找现有的 ChatConversationDTO
        ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);

        if (chatConversationDTO != null) {
            // 更新现有的消息列表
            chatConversationDTO.setMessages(messageArrays);
        } else {
            // 如果不存在，创建一个新的 ChatConversationDTO
            chatConversationDTO = new ChatConversationDTO();
            chatConversationDTO.setConversationId(conversationId);
            chatConversationDTO.setMessages(messageArrays);
        }

        // 保存更新后的 ChatConversationDTO
        mongoTemplate.save(chatConversationDTO);
        log.info("Replaced history for conversation ID: {} with {} messages.", conversationId, newMessages.size());
    }

    private String[] convertToArray(Message message) {
        return new String[]{message.getRole(), message.getTimestamp(), message.getContent()};
    }

}
