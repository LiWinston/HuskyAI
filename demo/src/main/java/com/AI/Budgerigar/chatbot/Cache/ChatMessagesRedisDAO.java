package com.AI.Budgerigar.chatbot.Cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.AI.Budgerigar.chatbot.Constant.ApplicationConstant.CONVERSATION_HISTORY_KEY_PREFIX;

@Repository
@Slf4j
public class ChatMessagesRedisDAO {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // private static final Logger logger =
    // Logger.getLogger(ChatMessagesRedisDAO.class.getName());

    // Get the number of messages.
    public long getMessageCount(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            Long count = redisTemplate.opsForList().size(key);
            return Objects.requireNonNullElse(count, 0L); // If count is equal to
                                                          // null，return 0.
        }
        catch (DataAccessException e) {
            log.error("Failed to get message count for conversation {}", conversationId, e);
            return 0; // Return 0 if an exception occurs.
        }
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    // Add a new message.
    public void addMessage(String conversationId, String speaker, String timestamp, String message) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        // String timestamp = Instant.now().toString().formatted(formatter);
        String messageWithTimestamp = speaker + "|" + timestamp + "|" + message;
        try {
            redisTemplate.opsForList().rightPush(key, messageWithTimestamp);
            // log.info("Message added to conversation {}: {}", conversationId,
            // messageWithTimestamp);
        }
        catch (DataAccessException e) {
            log.error("Failed to add message to conversation {}", conversationId, e);
        }
    }

    // Delete a message.
    public void deleteMessage(String conversationId, int index) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            redisTemplate.opsForList().set(key, index, "TO_BE_DELETED");
            redisTemplate.opsForList().remove(key, 1, "TO_BE_DELETED");
            log.info("Message deleted from conversation {} at index {}", conversationId, index);
        }
        catch (DataAccessException e) {
            log.error("Failed to delete message from conversation {} at index {}", conversationId, index, e);
        }
    }

    // Get chat history，return a string list.
    public List<String[]> getConversationHistory(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
            if (entries == null) {
                log.info("Redis:No history found for {}", conversationId);
                return List.of(); // Return an empty list instead of null.
            }
            else {
                return entries.stream()
                    .map(entry -> entry.split("\\|", 3)) // Split using the ":" delimiter
                                                         // to get the timestamp.
                    .map(parts -> new String[] { parts[0], parts[1], parts[2] }) // Handle
                                                                                 // messages
                                                                                 // with
                                                                                 // timestamps.
                    .collect(Collectors.toList());
            }
        }
        catch (DataAccessException e) {
            log.error("Failed to get conversation history for {}", conversationId, e);
            return List.of(); // Return an empty list to maintain method consistency.
        }
    }

    // Remove entire chat history.
    public Boolean clearConversation(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            redisTemplate.delete(key);
            log.info("Redis Cleared conversation history for conversationId: {}", conversationId);
            return true; // Successful operation，return "true".
        }
        catch (DataAccessException e) {
            log.error("Failed to clear conversation history for conversationId: {}", conversationId, e);
            return false; // Failure，return "false".
        }
    }

    @Autowired
    DateTimeFormatter dateTimeFormatter;

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    public void maintainMessageHistory(String conversationId) {
        log.info("Maintaining conversation history for {}", conversationId);
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;

        try {
            // Get chat history from Redis.
            List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
            if (entries == null || entries.isEmpty()) {
                log.info("Maintaining:No redis history found for {}", conversationId);
                return;
            }

            List<String> adjustedEntries = new ArrayList<>();
            Set<String> uniqueEntries = new HashSet<>(); // Used for deduplication.
            String lastRole = "";
            boolean needsAdjustment = false;

            // Remove duplicates and maintain alternating order.
            for (String entry : entries) {
                String[] parts = entry.split("\\|", 3);
                if (parts.length < 3) {
                    log.warn("Invalid message format, skipping: {}", entry);
                    continue;
                }

                String role = parts[0];
                String timestamp = parts[1];
                String content = parts[2];

                // If it's a duplicate message or the content is empty, skip it.
                if (uniqueEntries.contains(entry) || content.trim().isEmpty()) {
                    // log.info("Removed duplicate or empty message: {}", entry);
                    continue;
                }

                uniqueEntries.add(entry);

                // Check the interleaving order of messages.
                if (adjustedEntries.size() % 2 == 0) { // Even indices should be user
                                                       // messages.
                    if (role.equals("user")) {
                        adjustedEntries.add(entry);
                        lastRole = role;
                    }
                    else {
                        needsAdjustment = true;
                        log.warn("Expected user message, but found assistant. Skipping entry.");
                    }
                }
                else { // Odd indices should be assistant messages.
                    if (role.equals("assistant")) {
                        adjustedEntries.add(entry);
                        lastRole = role;
                    }
                    else {
                        needsAdjustment = true;
                        log.warn("Expected assistant message, but found user. Skipping entry.");
                    }
                }
            }

            // Ensure the total is even and the assistant ends.
            if (adjustedEntries.size() % 2 == 1 || !lastRole.equals("assistant")) {
                String lastTimestamp = getNowTimeStamp();
                adjustedEntries.add("assistant|" + lastTimestamp + "| ");
                log.info("Added placeholder to ensure conversation ends with assistant.");
            }

            // Update conversation history in Redis.
            redisTemplate.delete(key);
            redisTemplate.opsForList().rightPushAll(key, adjustedEntries);
            log.info("Maintained history adjusted for {}", conversationId);

        }
        catch (DataAccessException e) {
            log.error("Failed to maintain history for {}", conversationId, e);
        }
    }

    // Get the latest N conversation history.
    public List<String[]> getLastNMessages(String conversationId, int n) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            List<String> entries = redisTemplate.opsForList().range(key, -n, -1); // Get
                                                                                  // the
                                                                                  // latest
                                                                                  // N
                                                                                  // messages.
            if (entries == null) {
                log.info("No history found for {}", conversationId);
                return Arrays.asList();
            }
            else {
                return entries.stream()
                    .map(entry -> entry.split("\\|", 3)) // Split using the ":" delimiter
                                                         // to get the timestamp.
                    .map(parts -> new String[] { parts[0], parts[1], parts[2] }) // Handle
                                                                                 // messages
                                                                                 // with
                                                                                 // timestamps.
                    .collect(Collectors.toList());
            }
        }
        catch (DataAccessException e) {
            log.error("Failed to get last N messages for {}", conversationId, e);
            return Arrays.asList();
        }
    }

    // Obtain conversation history within a specified range, suitable for progressive
    // loading or pagination. - Unchecked
    public List<String[]> getMessagesRange(String conversationId, int start, int end) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            List<String> entries = redisTemplate.opsForList().range(key, start, end);
            if (entries == null) {
                log.info("No history found for {}", conversationId);
                return List.of();
            }
            else {
                return entries.stream()
                    .map(entry -> entry.split("\\|", 3)) // Split using the ":" delimiter
                                                         // to get the timestamp.
                    .map(parts -> new String[] { parts[0], parts[1], parts[2] }) // Handle
                                                                                 // messages
                                                                                 // with
                                                                                 // timestamps.
                    .collect(Collectors.toList());
            }
        }
        catch (DataAccessException e) {
            log.error("Failed to get messages in range for {}", conversationId, e);
            return List.of();
        }
    }

}
