package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAOImpl;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ChatSyncServiceImpl implements ChatSyncService {

    @Autowired
    private ChatMessagesMongoDAOImpl chatMessagesMongoDAO;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    //get历史传给前端显示，何尝不是一种同步捏
    @Override
    public List<Message> getHistory(String conversationId) {
        return chatMessagesMongoDAO.getConversationHistory(conversationId);
    }

    // 更新历史记录，从 Redis 获取最新消息并更新到 MongoDB
    public void updateHistoryFromRedis(String conversationId, int numberOfEntries) {
        List<Message> newEntries = chatMessagesRedisDAO.getConversationHistory(conversationId).stream()
                .map(this::convertToMessage)
                .skip(Math.max(0, chatMessagesRedisDAO.getConversationHistory(conversationId).size() - numberOfEntries))
                .toList();
        chatMessagesMongoDAO.updateHistoryById(conversationId, newEntries);
    }

    @Transactional
    // 更新历史记录，从 Redis 获取所有消息, 比照存储，综合处理重复消息并全量更新到 MongoDB
    public void updateHistoryFromRedis(String conversationId) {
        log.info("Starting sync operation from Redis to MongoDB for conversation ID: {}", conversationId);

        // 获取 Redis 和 MongoDB 中的对话历史
        List<Message> redisMessages = chatMessagesRedisDAO.getConversationHistory(conversationId).stream()
                .map(this::convertToMessage)
                .toList();
        List<Message> mongoMessages = chatMessagesMongoDAO.getConversationHistory(conversationId);

        // 使用Set来跟踪消息的唯一性，避免重复
        Set<String> uniqueMessagesSet = new HashSet<>();
        List<Message> newMessagesToPersist = new ArrayList<>();
        List<Integer> duplicateIndexesInMongo = new ArrayList<>();

        // 检查和标记MongoDB中的重复消息
        for (int i = 0; i < mongoMessages.size(); i++) {
            Message mongoMessage = mongoMessages.get(i);
            String messageKey = mongoMessage.getRole() + mongoMessage.getTimestamp() + mongoMessage.getContent();
            if (uniqueMessagesSet.contains(messageKey)) {
                duplicateIndexesInMongo.add(i);  // 标记重复消息的索引
            } else {
                uniqueMessagesSet.add(messageKey);
            }
        }

        // 合并Redis中的消息并确保唯一性
        for (int i = 0; i < redisMessages.size(); i += 2) {
            if (i + 1 >= redisMessages.size()) {
                log.warn("Incomplete message pair found in Redis for conversation ID: {} at index: {}", conversationId, i);
                break;  // 如果发现不完整的消息对儿，退出循环
            }

            Message userMessage = redisMessages.get(i);
            Message assistantMessage = redisMessages.get(i + 1);

            // 确保消息是合法的时间递增顺序
            if (isValidMessagePair(userMessage, assistantMessage)) {
                String userMessageKey = userMessage.getRole() + userMessage.getTimestamp() + userMessage.getContent();
                String assistantMessageKey = assistantMessage.getRole() + assistantMessage.getTimestamp() + assistantMessage.getContent();

                if (!uniqueMessagesSet.contains(userMessageKey)) {
                    uniqueMessagesSet.add(userMessageKey);
                    newMessagesToPersist.add(userMessage);  // 新消息加入
                }

                if (!uniqueMessagesSet.contains(assistantMessageKey)) {
                    uniqueMessagesSet.add(assistantMessageKey);
                    newMessagesToPersist.add(assistantMessage);  // 新消息加入
                }
            } else {
                log.warn("Invalid message pair found for conversation ID: {} at Redis index: {}", conversationId, i);
            }
        }

        // 删除MongoDB中的重复消息，从后往前删除
        if (!duplicateIndexesInMongo.isEmpty()) {
            log.info("Removing {} duplicate messages from MongoDB for conversation ID: {}", duplicateIndexesInMongo.size(), conversationId);
            // 按照倒序删除重复的消息
            for (int i = duplicateIndexesInMongo.size() - 1; i >= 0; i--) {
                int index = duplicateIndexesInMongo.get(i);
                mongoMessages.remove(index);
            }
        }


        // 将新的合法消息加入MongoDB
        if (!newMessagesToPersist.isEmpty()) {
            log.info("Persisting {} new message pairs to MongoDB for conversation ID: {}", newMessagesToPersist.size() / 2, conversationId);
            mongoMessages.addAll(newMessagesToPersist);
        }

        // 按照时间戳排序所有消息
        mongoMessages.sort((m1, m2) -> parseTimestamp(m1.getTimestamp()).compareTo(parseTimestamp(m2.getTimestamp())));

        // 保存更新后的消息列表到MongoDB
        chatMessagesMongoDAO.replaceHistoryById(conversationId, mongoMessages);
    }

    private Instant parseTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            log.warn("Invalid timestamp format: {}", timestamp, e);
            return Instant.MIN; // 返回一个最小值以表示错误的时间戳
        }
    }



    // 辅助方法，用于检查消息对儿是否合法
    private boolean isValidMessagePair(Message userMessage, Message assistantMessage) {
        return userMessage.getRole().equals("user")
                && assistantMessage.getRole().equals("assistant")
                && Instant.parse(userMessage.getTimestamp()).isBefore(Instant.parse(assistantMessage.getTimestamp()));
    }

    // 更新历史记录，从 MongoDB 获取较早的消息并更新到 Redis
    @Override
    public void updateRedisFromMongo(String conversationId) {
        log.info("Starting sync operation from MongoDB to Redis for conversation ID: {}", conversationId);

        // 获取 Redis 和 MongoDB 中的对话历史
        List<Message> redisMessages = chatMessagesRedisDAO.getConversationHistory(conversationId).stream()
                .map(this::convertToMessage)
                .toList();
        List<Message> mongoMessages = chatMessagesMongoDAO.getConversationHistory(conversationId);

        log.info("Redis message count: {}, MongoDB message count: {}", redisMessages.size(), mongoMessages.size());

        // 合并 MongoDB 和 Redis 的消息，避免覆盖较新的 Redis 消息
        List<Message> mergedMessages = mergeMessages(redisMessages, mongoMessages);

        // 将合并后的消息写回 Redis
        chatMessagesRedisDAO.clearConversation(conversationId); // 清空现有 Redis 数据
        mergedMessages.forEach(message -> chatMessagesRedisDAO.addMessage(
                conversationId, message.getRole(), message.getTimestamp(), message.getContent()));

        log.info("Sync operation from MongoDB to Redis completed for conversation ID: {}", conversationId);
    }

    private Message convertToMessage(String[] messageParts) {
        if (messageParts.length != 3) {
            throw new IllegalArgumentException("Invalid message format. Expected [role, timestamp, content]");
        }
        String role = messageParts[0];
        String timestamp = messageParts[1];
        String content = messageParts[2];

        // 验证时间戳格式
        try {
            Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            log.error("Invalid timestamp format: {}", timestamp);
            throw new IllegalArgumentException("Invalid timestamp format", e);
        }

        return new Message(role, timestamp, content);
    }

    // 合并 Redis 和 MongoDB 消息，基于时间戳
    private List<Message> mergeMessages(List<Message> redisMessages, List<Message> mongoMessages) {
        List<Message> mergedMessages = new ArrayList<>();
        int redisIndex = 0;
        int mongoIndex = 0;

        // 使用双指针法合并两个有序列表
        while (redisIndex < redisMessages.size() && mongoIndex < mongoMessages.size()) {
            Message redisMessage = redisMessages.get(redisIndex);
            Message mongoMessage = mongoMessages.get(mongoIndex);

            Instant redisTimestamp = Instant.parse(redisMessage.getTimestamp());
            Instant mongoTimestamp = Instant.parse(mongoMessage.getTimestamp());

            if (redisTimestamp.isBefore(mongoTimestamp)) {
                mergedMessages.add(redisMessage);
                redisIndex++;
            } else {
                mergedMessages.add(mongoMessage);
                mongoIndex++;
            }
        }

        // 添加剩余的 Redis 消息
        while (redisIndex < redisMessages.size()) {
            mergedMessages.add(redisMessages.get(redisIndex++));
        }

        // 添加剩余的 Mongo 消息
        while (mongoIndex < mongoMessages.size()) {
            mergedMessages.add(mongoMessages.get(mongoIndex++));
        }

        return mergedMessages;
    }
}
