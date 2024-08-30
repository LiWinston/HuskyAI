package com.AI.Budgerigar.chatbot.Cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.AI.Budgerigar.chatbot.Constant.ApplicationConstant.CONVERSATION_HISTORY_KEY_PREFIX;

@Repository
public class ChatMessagesRedisDAO {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final Logger logger = Logger.getLogger(ChatMessagesRedisDAO.class.getName());

    // 添加新消息
    public void addMessage(String conversationId, String speaker, String message) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            redisTemplate.opsForList().rightPush(key, speaker + ":" + message);
            logger.info("Message added to conversation " + conversationId);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Failed to add message to conversation " + conversationId, e);
        }
    }

    // 删除某条消息
    public void deleteMessage(String conversationId, int index) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            redisTemplate.opsForList().set(key, index, "TO_BE_DELETED");
            redisTemplate.opsForList().remove(key, 1, "TO_BE_DELETED");
            logger.info("Message deleted from conversation " + conversationId + " at index " + index);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Failed to delete message from conversation " + conversationId + " at index " + index, e);
        }
    }

    // 获取对话历史，返回 List<String[]> 类型
    public List<String[]> getConversationHistory(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
            if (entries == null) {
                logger.warning("No conversation history found for " + conversationId);
                return List.of(); // 返回空列表而不是null
            }else{
                logger.info("Retrieved conversation history for " + conversationId);
                return entries.stream()
                        .map(entry -> entry.split(":", 2)) // 解析 "role:message" 格式
                        .collect(Collectors.toList());
            }
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Failed to retrieve conversation history for " + conversationId, e);
            return List.of(); // 返回空列表以保持方法一致性
        }
    }

    // 清除整个对话历史
    public void clearConversation(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            redisTemplate.delete(key);
            logger.info("Cleared conversation history for " + conversationId);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Failed to clear conversation history for " + conversationId, e);
        }
    }

    // 以维护对话历史，若对话由于某种原因中断，可确保奇偶性和角色正确
    // 若总个数为奇数，最后发言的是user，则删除最近添加的消息
    // 若总个数为偶数，最后发言的是user, 则说明对话已乱序，遍历消息并做出调整，确保总数为偶的同时保持角色正确，允许添加空消息
    public void maintainMessageHistory(String conversationId) {
        logger.info("Maintaining conversation history for " + conversationId);
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;

        try {
            // 获取对话历史列表
            List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
            if (entries == null || entries.isEmpty()) {
                logger.warning("No conversation history found for " + conversationId);
                return;
            }

            int size = entries.size();

            // 如果对话条目总数为奇数且最后一个是用户消息，则删除最后一条消息
            if (size % 2 == 1 && entries.get(size - 1).startsWith("user")) {
                redisTemplate.opsForList().rightPop(key);
                logger.info("Removed the last user message from conversation " + conversationId);
            } else if (size % 2 == 0) {
                // 检查对话是否按正确顺序交替（user -> assistant）
                boolean needsAdjustment = false;

                for (int i = 0; i < size; i += 2) {
                    String userMessage = entries.get(i);
                    if (!userMessage.startsWith("user")) {
                        needsAdjustment = true;
                        break;
                    }
                    // 检查是否有对应的 assistant 消息
                    if (i + 1 < size) {
                        String assistantMessage = entries.get(i + 1);
                        if (!assistantMessage.startsWith("assistant")) {
                            needsAdjustment = true;
                            break;
                        }
                    }
                }

                if (needsAdjustment) {
                    logger.info("Adjusting conversation history for " + conversationId);
                    List<String> adjustedEntries = new ArrayList<>();

                    for (int i = 0; i < size; i++) {
                        String entry = entries.get(i);
                        // 确保对话的交替顺序
                        if (i % 2 == 0) {
                            // 偶数索引应为用户消息
                            if (!entry.startsWith("user")) {
                                adjustedEntries.add("user: ");
                            } else {
                                adjustedEntries.add(entry);
                            }
                        } else {
                            // 奇数索引应为 assistant 消息
                            if (!entry.startsWith("assistant")) {
                                adjustedEntries.add("assistant: ");
                            } else {
                                adjustedEntries.add(entry);
                            }
                        }
                    }

                    // 清空原始列表并保存调整后的对话
                    redisTemplate.delete(key);
                    redisTemplate.opsForList().rightPushAll(key, adjustedEntries);
                    logger.info("Conversation history adjusted for " + conversationId);
                } else {
                    logger.info("Conversation history is already consistent for " + conversationId);
                }
            }
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Failed to maintain conversation history for " + conversationId, e);
        }
    }

}
