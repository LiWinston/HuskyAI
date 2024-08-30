package com.AI.Budgerigar.chatbot.Cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    // 删除最近添加的消息
    public void deleteLastMessage(String conversationId) {
        String key = CONVERSATION_HISTORY_KEY_PREFIX + conversationId;
        try {
            redisTemplate.opsForList().rightPop(key);
            logger.info("Last message deleted from conversation " + conversationId);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Failed to delete last message from conversation " + conversationId, e);
        }
    }
}
