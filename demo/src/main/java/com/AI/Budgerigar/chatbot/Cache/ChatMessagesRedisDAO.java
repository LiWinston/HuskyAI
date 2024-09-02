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

    // 单纯获取消息条数
    public long getMessageCount(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            Long count = redisTemplate.opsForList().size(key);
            return Objects.requireNonNullElse(count, 0L); // 如果 count 是 null，则返回 0
        }
        catch (DataAccessException e) {
            log.error("Failed to get message count for conversation {}", conversationId, e);
            return 0; // 出现异常时返回0
        }
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    // 添加新消息（更新后的方法）
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

    // 删除某条消息
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

    // 获取对话历史，返回 List<String[]> 类型
    public List<String[]> getConversationHistory(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
            if (entries == null) {
                log.info("Redis:No history found for {}", conversationId);
                return List.of(); // 返回空列表而不是null
            }
            else {
                return entries.stream()
                    .map(entry -> entry.split("\\|", 3)) // 使用 ":" 分隔符拆分，获取时间戳
                    .map(parts -> new String[] { parts[0], parts[1], parts[2] }) // 处理带时间戳的消息
                    .collect(Collectors.toList());
            }
        }
        catch (DataAccessException e) {
            log.error("Failed to get conversation history for {}", conversationId, e);
            return List.of(); // 返回空列表以保持方法一致性
        }
    }

    // 清除整个对话历史
    public void clearConversation(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            redisTemplate.delete(key);
            log.info("Cleared conversation history for {}", conversationId);
        }
        catch (DataAccessException e) {
            log.error("Failed to clear conversation history for {}", conversationId, e);
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
            // 获取对话历史列表
            List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
            if (entries == null || entries.isEmpty()) {
                log.info("Maintaining:No redis history found for {}", conversationId);
                return;
            }

            List<String> adjustedEntries = new ArrayList<>();
            Set<String> uniqueEntries = new HashSet<>(); // 用于去重
            String lastRole = "";
            boolean needsAdjustment = false;

            // 去重并维护交替顺序
            for (String entry : entries) {
                String[] parts = entry.split("\\|", 3);
                if (parts.length < 3) {
                    log.warn("Invalid message format, skipping: {}", entry);
                    continue;
                }

                String role = parts[0];
                String timestamp = parts[1];
                String content = parts[2];

                // 如果是重复的消息或内容为空，跳过
                if (uniqueEntries.contains(entry) || content.trim().isEmpty()) {
                    // log.info("Removed duplicate or empty message: {}", entry);
                    continue;
                }

                uniqueEntries.add(entry);

                // 检查消息交替顺序
                if (adjustedEntries.size() % 2 == 0) { // 偶数索引应为用户消息
                    if (role.equals("user")) {
                        adjustedEntries.add(entry);
                        lastRole = role;
                    }
                    else {
                        needsAdjustment = true;
                        log.warn("Expected user message, but found assistant. Skipping entry.");
                    }
                }
                else { // 奇数索引应为助手消息
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

            // 保证总数为偶数且助手结束
            if (adjustedEntries.size() % 2 == 1 || !lastRole.equals("assistant")) {
                String lastTimestamp = getNowTimeStamp();
                adjustedEntries.add("assistant|" + lastTimestamp + "| ");
                log.info("Added placeholder to ensure conversation ends with assistant.");
            }

            // 更新 Redis 中的对话历史
            redisTemplate.delete(key);
            redisTemplate.opsForList().rightPushAll(key, adjustedEntries);
            log.info("Maintained history adjusted for {}", conversationId);

        }
        catch (DataAccessException e) {
            log.error("Failed to maintain history for {}", conversationId, e);
        }
    }

    // 获取最近 N 条对话历史
    public List<String[]> getLastNMessages(String conversationId, int n) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            List<String> entries = redisTemplate.opsForList().range(key, -n, -1); // 获取最后
                                                                                  // N 条消息
            if (entries == null) {
                log.info("No history found for {}", conversationId);
                return Arrays.asList();
            }
            else {
                return entries.stream()
                    .map(entry -> entry.split("\\|", 3)) // 使用 ":" 分隔符拆分
                    .map(parts -> new String[] { parts[0], parts[1], parts[2] }) // 返回带时间戳的消息
                    .collect(Collectors.toList());
            }
        }
        catch (DataAccessException e) {
            log.error("Failed to get last N messages for {}", conversationId, e);
            return Arrays.asList();
        }
    }

    // 获取指定范围内的对话历史,适用于渐进式加载或分页 - Unchecked
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
                    .map(entry -> entry.split("\\|", 3)) // 使用 ":" 分隔符拆分
                    .map(parts -> new String[] { parts[0], parts[1], parts[2] }) // 返回带时间戳的消息
                    .collect(Collectors.toList());
            }
        }
        catch (DataAccessException e) {
            log.error("Failed to get messages in range for {}", conversationId, e);
            return List.of();
        }
    }

}
