package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAOImpl;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.result.Result;
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

    @Autowired
    private UserMapper userMapper;

    // Get history and pass it to the front end for display.
    @Override
    public List<Message> getHistory(String conversationId) {
        // List<String[]> conversationHistory =
        // chatMessagesRedisDAO.getConversationHistory(conversationId);
        // if (!conversationHistory.isEmpty()) {
        // return conversationHistory.stream().map(this::convertToMessage).toList();
        // }
        // // First, submit current conversation cache to DB
        updateHistoryFromRedis(conversationId);
        return chatMessagesMongoDAO.getConversationHistory(conversationId);
        //// updateRedisFromMongo(conversationId);
        // // Get history to show in front end
        // return chatMessagesRedisDAO.getConversationHistory(conversationId)
        // .stream()
        // .map(this::convertToMessage)
        // .toList();
    }

    // Update the history log, fetch the latest messages from Redis, and update them to
    // MongoDB.
    public void updateHistoryFromRedis(String conversationId, int numberOfEntries) {
        List<Message> newEntries = chatMessagesRedisDAO.getConversationHistory(conversationId)
            .stream()
            .map(this::convertToMessage)
            .skip(Math.max(0, chatMessagesRedisDAO.getConversationHistory(conversationId).size() - numberOfEntries))
            .toList();
        chatMessagesMongoDAO.updateHistoryById(conversationId, newEntries);
    }

    @Transactional
    // Update the history by retrieving all messages from Redis, compare and store,
    // comprehensively process duplicate messages, and fully update to MongoDB.
    public void updateHistoryFromRedis(String conversationId) {
        log.info("Starting sync operation from Redis to MongoDB for conversation ID: {}", conversationId);

        // Obtain conversation history from Redis and MongoDB.
        List<Message> redisMessages = chatMessagesRedisDAO.getConversationHistory(conversationId)
            .stream()
            .map(this::convertToMessage)
            .toList();

        if (redisMessages.isEmpty()) {
            log.warn("Cache is empty: {}", conversationId);
            // Use probability to decide whether to maintain history for Mongo.
            if (Math.random() < 0.15) {
                log.info("MongoDB randomly maintains history: {}", conversationId);
                chatMessagesMongoDAO.updateHistoryById(conversationId, redisMessages);
            }
        }

        List<Message> mongoMessages = chatMessagesMongoDAO.getConversationHistory(conversationId);

        if (mongoMessages.isEmpty()) {
            log.info("MongoDB is empty: {}", conversationId);
            chatMessagesMongoDAO.replaceHistoryById(conversationId, redisMessages);
            return;
        }
        // Use a Set to track the uniqueness of messages and avoid duplication.
        Set<String> uniqueMessagesSet = new HashSet<>();
        List<Message> newMessagesToPersist = new ArrayList<>();
        List<Integer> duplicateIndexesInMongo = new ArrayList<>();

        // Check and mark duplicate messages in MongoDB.
        for (int i = 0; i < mongoMessages.size(); i++) {
            Message mongoMessage = mongoMessages.get(i);
            String messageKey = mongoMessage.getRole() + mongoMessage.getTimestamp() + mongoMessage.getContent();
            if (uniqueMessagesSet.contains(messageKey)) {
                duplicateIndexesInMongo.add(i); // Index of duplicate messages.
            }
            else {
                uniqueMessagesSet.add(messageKey);
            }
        }

        // Merge messages in Redis and ensure uniqueness.
        for (int i = 0; i < redisMessages.size(); i += 2) {
            if (i + 1 >= redisMessages.size()) {
                log.warn("Incomplete message pair found in Redis for conversation ID: {} at index: {}", conversationId,
                        i);
                break; // If incomplete message pairs are found, exit the loop.
            }

            Message userMessage = redisMessages.get(i);
            Message assistantMessage = redisMessages.get(i + 1);

            // Ensure that messages are in a legally time-incrementing order.
            if (isValidMessagePair(userMessage, assistantMessage)) {
                String userMessageKey = userMessage.getRole() + userMessage.getTimestamp() + userMessage.getContent();
                String assistantMessageKey = assistantMessage.getRole() + assistantMessage.getTimestamp()
                        + assistantMessage.getContent();

                if (!uniqueMessagesSet.contains(userMessageKey)) {
                    uniqueMessagesSet.add(userMessageKey);
                    newMessagesToPersist.add(userMessage); // New message added.
                }

                if (!uniqueMessagesSet.contains(assistantMessageKey)) {
                    uniqueMessagesSet.add(assistantMessageKey);
                    newMessagesToPersist.add(assistantMessage); // New message added.
                }
            }
            else {
                log.warn("Invalid message pair found for conversation ID: {} at Redis index: {}", conversationId, i);
            }
        }

        // Delete duplicate messages in MongoDB from back to front.
        if (!duplicateIndexesInMongo.isEmpty()) {
            log.info("Removing {} duplicate messages from MongoDB for conversation ID: {}",
                    duplicateIndexesInMongo.size(), conversationId);
            // Delete duplicate messages in reverse order.
            for (int i = duplicateIndexesInMongo.size() - 1; i >= 0; i--) {
                int index = duplicateIndexesInMongo.get(i);
                mongoMessages.remove(index);
            }
        }

        // Add new messages to the MongoDB message list.
        if (!newMessagesToPersist.isEmpty()) {
            log.info("Persisting {} new message pairs to MongoDB for conversation ID: {}",
                    newMessagesToPersist.size() / 2, conversationId);
            mongoMessages.addAll(newMessagesToPersist);
        }

        // Sort all messages by timestamp.
        mongoMessages.sort((m1, m2) -> parseTimestamp(m1.getTimestamp()).compareTo(parseTimestamp(m2.getTimestamp())));
        log.info("Messages sorted : {}", conversationId);

        try {
            // Save the updated message list to MongoDB.
            chatMessagesMongoDAO.replaceHistoryById(conversationId, mongoMessages);
        }
        catch (Exception e) {
            log.error("Error occurred while updating MongoDB for conversation ID: {}", conversationId, e);
            throw new RuntimeException("Error updating MongoDB in " + getClass(), e);
        }
    }

    @Override
    @Transactional
    public Result<?> deleteConversation(String uuid, String conversationId) {
        try {
            // 1. MongoDB delete operation
            boolean mongoDeleted = chatMessagesMongoDAO.deleteConversationById(conversationId);
            if (!mongoDeleted) {
                log.error("Failed to delete conversation in MongoDB for conversation ID: {}", conversationId);
                return Result.error("MongoDB delete operation failed");
            }

            // 2. Redis cache clear
            Boolean redisCleared = chatMessagesRedisDAO.clearConversation(conversationId);
            if (!redisCleared) {
                log.error("Failed to clear conversation in Redis for conversation ID: {}", conversationId);
                return Result.error("Redis clear operation failed");
            }

            // 3. SQL delete operation
            int rowsAffected = userMapper.deleteConversationByUuidCid(uuid, conversationId);
            if (rowsAffected == 0) {
                log.error("Failed to delete conversation in MySQL for UUID: {}, conversation ID: {}", uuid,
                        conversationId);
                return Result.error("MySQL delete operation failed");
            }
            else {
                log.info("SQL delete operation success for UUID: {}, conversation ID: {}", uuid, conversationId);
            }

            // 4. All operations succeeded, returning success result.
            return Result.success(null, "Conversation deleted successfully");
        }
        catch (Exception e) {
            // Capture and record exceptions.
            log.error("Error occurred while deleting conversation ID: {} for UUID: {}", conversationId, uuid, e);
            return Result.error("Error occurred while deleting conversation");
        }
    }

    private Instant parseTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp);
        }
        catch (DateTimeParseException e) {
            log.warn("Invalid timestamp format: {}", timestamp, e);
            return Instant.MIN; // Return a minimum value to indicate an error timestamp.
        }
    }

    // Auxiliary method for checking if a message pair is valid.
    private boolean isValidMessagePair(Message userMessage, Message assistantMessage) {
        return userMessage.getRole().equals("user") && assistantMessage.getRole().equals("assistant")
                && Instant.parse(userMessage.getTimestamp()).isBefore(Instant.parse(assistantMessage.getTimestamp()));
    }

    // Update the history log, retrieve earlier messages from MongoDB, and update them to
    // Redis.
    @Override
    public void updateRedisFromMongo(String conversationId) {
        log.info("Starting sync operation from MongoDB to Redis for conversation ID: {}", conversationId);

        // Retrieve conversation history from Redis and MongoDB.
        List<Message> redisMessages = chatMessagesRedisDAO.getConversationHistory(conversationId)
            .stream()
            .map(this::convertToMessage)
            .toList();
        List<Message> mongoMessages = chatMessagesMongoDAO.getConversationHistory(conversationId);

        log.info("Redis message count: {}, MongoDB message count: {}", redisMessages.size(), mongoMessages.size());

        // Merge the messages of MongoDB and Redis, avoiding overwriting newer Redis
        // messages.
        List<Message> mergedMessages = mergeMessages(redisMessages, mongoMessages);
        for (Message message : mergedMessages) {
            log.info("Merged message: {}", message);
        }

        // Write the merged message back to Redis.
        chatMessagesRedisDAO.clearConversation(conversationId); // Clear existing Redis
                                                                // data.
        mergedMessages.forEach(message -> chatMessagesRedisDAO.addMessage(conversationId, message.getRole(),
                message.getTimestamp(), message.getContent()));

        log.info("Sync operation from MongoDB to Redis completed for conversation ID: {}", conversationId);
    }

    private Message convertToMessage(String[] messageParts) {
        if (messageParts.length != 3) {
            throw new IllegalArgumentException("Invalid message format. Expected [role, timestamp, content]");
        }
        String role = messageParts[0];
        String timestamp = messageParts[1];
        String content = messageParts[2];

        // Verify timestamp format.
        try {
            Instant.parse(timestamp);
        }
        catch (DateTimeParseException e) {
            log.error("Invalid timestamp format: {}", timestamp);
            throw new IllegalArgumentException("Invalid timestamp format", e);
        }

        return new Message(role, timestamp, content);
    }

    // Merge Redis and MongoDB messages based on timestamps.
    private List<Message> mergeMessages(List<Message> redisMessages, List<Message> mongoMessages) {
        List<Message> mergedMessages = new ArrayList<>();
        int redisIndex = 0;
        int mongoIndex = 0;

        // Use the two-pointer method to merge two sorted lists.
        while (redisIndex < redisMessages.size() && mongoIndex < mongoMessages.size()) {
            Message redisMessage = redisMessages.get(redisIndex);
            Message mongoMessage = mongoMessages.get(mongoIndex);

            Instant redisTimestamp = Instant.parse(redisMessage.getTimestamp());
            Instant mongoTimestamp = Instant.parse(mongoMessage.getTimestamp());

            if (redisTimestamp.isBefore(mongoTimestamp)) {
                mergedMessages.add(redisMessage);
                redisIndex++;
            }
            else {
                mergedMessages.add(mongoMessage);
                mongoIndex++;
            }
        }

        // Add remaining Redis messages.
        while (redisIndex < redisMessages.size()) {
            mergedMessages.add(redisMessages.get(redisIndex++));
        }

        // Add the remaining Mongo messages.
        while (mongoIndex < mongoMessages.size()) {
            mergedMessages.add(mongoMessages.get(mongoIndex++));
        }

        return mergedMessages;
    }

}
