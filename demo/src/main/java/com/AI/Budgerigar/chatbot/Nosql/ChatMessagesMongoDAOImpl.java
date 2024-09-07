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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class ChatMessagesMongoDAOImpl implements ChatMessagesMongoDAO {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public int getConversationLengthById(String conversationId) {
        try {
            MatchOperation match = Aggregation.match(Criteria.where("_id").is(conversationId));
            ProjectionOperation project = Aggregation.project().and("messages").size().as("messageCount");
            Aggregation aggregation = Aggregation.newAggregation(match, project);

            AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, "chat_conversations",
                    Document.class);
            Document document = result.getUniqueMappedResult();

            if (document != null) {
                return document.getInteger("messageCount", 0);
            }
            else {
                return 0;
            }

        }
        catch (Exception e) {
            log.error("MongoDB_GET_LENGTH : Failed to get conversation length for conversation ID: {}", conversationId,
                    e);
            // 用传统方法获取
            ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);
            if (chatConversationDTO != null) {
                return chatConversationDTO.getMessages().size();
            }
            else {
                return 0;
            }
        }
    }

    @Override
    public void updateHistoryById(String conversationId, List<Message> newMessages) {
        List<String[]> messageArrays = newMessages.stream().map(this::convertToArray).collect(Collectors.toList());

        try {
            ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);
            if (chatConversationDTO != null) {
                chatConversationDTO.addStringMessages(messageArrays); // 使用字符串数组
                mongoTemplate.save(chatConversationDTO);
            }
            else {
                ChatConversationDTO newChatConversationDTO = new ChatConversationDTO();
                newChatConversationDTO.setMessages(messageArrays);
                newChatConversationDTO.setConversationId(conversationId);
                mongoTemplate.save(newChatConversationDTO);
            }
        }
        catch (Exception e) {
            log.error("Mongo_UPD_Simple : Failed to update (简单方法) for conversation ID: {}", conversationId, e);
        }
    }

    @Override
    public List<Message> getConversationHistory(String conversationId) {
        // Step 1: 尝试从MongoDB中获取会话记录
        try {
            ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);

            // Step 2: 检查会话记录是否存在
            if (chatConversationDTO != null) {
                List<String[]> messagesList = chatConversationDTO.getMessages();

                // Step 3: 确认消息列表的结构和内容是否有效
                if (messagesList != null && !messagesList.isEmpty() && messagesList.get(0) != null) {
                    log.info("MongoDB_Get_History:Found {} history - {}. Returning {} messages.", messagesList.size(),
                            conversationId, messagesList.size());

                    // Step 4: 转换消息列表为Message对象列表并返回
                    return messagesList.stream().map(this::convertToMessage).collect(Collectors.toList());
                }
                else {
                    // 错误: 消息结构无效
                    log.error("Invalid message structure for conversation ID: " + conversationId);
                    return new ArrayList<>(); // 返回一个可变的空列表
                }
            }
            else {
                // 错误: 没有找到对应的会话记录
                log.error("MongoDB_Get_History:No history - {}. Returning empty list.", conversationId);
                return new ArrayList<>(); // 返回一个可变的空列表
            }
        }
        catch (Exception e) {
            // Step 5: 异常处理 - 捕获并记录错误信息
            log.error("Error fetching conversation history for ID: " + conversationId + ", Error: " + e.getMessage(),
                    e);
            return new ArrayList<>(); // 返回一个可变的空列表以避免异常传播
        }
    }

    // 将 String[] 转换为 Message 对象的方法
    private Message convertToMessage(String[] messageArray) {
        if (messageArray.length == 3) {
            return new Message(messageArray[0], messageArray[1], messageArray[2]);
        }
        else {
            throw new IllegalArgumentException("Invalid message format. Expected [role, timestamp, content]");
        }
    }

    @Override
    public void replaceHistoryById(String conversationId, List<Message> newMessages) {
        // 将 Message 对象列表转换为 String[] 的列表
        List<String[]> messageArrays = newMessages.stream().map(this::convertToArray).collect(Collectors.toList());

        // 查找现有的 ChatConversationDTO
        try {
            ChatConversationDTO chatConversationDTO = mongoTemplate.findById(conversationId, ChatConversationDTO.class);

            if (chatConversationDTO != null) {
                // 更新现有的消息列表
                chatConversationDTO.setMessages(messageArrays);
            }
            else {
                // 如果不存在，创建一个新的 ChatConversationDTO
                chatConversationDTO = new ChatConversationDTO();
                chatConversationDTO.setConversationId(conversationId);
                chatConversationDTO.setMessages(messageArrays);
            }

            // 保存更新后的 ChatConversationDTO
            mongoTemplate.save(chatConversationDTO);
            log.info("Replaced history for conversation ID: {} with {} messages.", conversationId, newMessages.size());
        }
        catch (Exception e) {
            log.error("Failed to replace history for conversation ID: {}", conversationId, e);
            throw e;
        }

    }

    @Override
    public Boolean deleteConversationById(String conversationId) {
        try {
            mongoTemplate.remove(new Query(Criteria.where("_id").is(conversationId)), ChatConversationDTO.class);
            log.info("Mongo Deleted conversation with ID: {}", conversationId);
            return true;
        }
        catch (Exception e) {
            log.error("Failed to delete conversation with ID: {}", conversationId, e);
            return false;
        }
    }

    private String[] convertToArray(Message message) {
        return new String[] { message.getRole(), message.getTimestamp(), message.getContent() };
    }

}
