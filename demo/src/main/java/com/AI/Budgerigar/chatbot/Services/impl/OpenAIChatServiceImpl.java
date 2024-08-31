package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.ChatRequestDTO;
import com.AI.Budgerigar.chatbot.DTO.ChatResponseDTO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OpenAIChatServiceImpl implements ChatService {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    @Setter
    @Getter
    public String conversationId;
    @Autowired
    DateTimeFormatter dateTimeFormatter;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;
    @Autowired
    private ChatMessagesMongoDAO chatMessagesMongoDAO;
    @Autowired
    private ChatSyncServiceImpl chatSyncService;
    @Value("${openai.model}")
    private String model;
    @Value("${openai.api.url}")
    private String apiUrl;

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @PostConstruct
    public void init() {
        conversationId = "default_openai_conversation";
    }

    @Override
    public String chat(String prompt) throws Exception {
        chatSyncService.updateRedisFromMongo(conversationId);

        chatMessagesRedisDAO.maintainMessageHistory(conversationId);

        // 将用户的输入添加到 Redis 会话历史
        chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(), prompt);

        // 从 Redis 获取完整的会话历史
        List<String[]> conversationHistory = chatMessagesRedisDAO.getConversationHistory(conversationId);

        // 使用工厂方法从 String[] 列表创建 ChatRequestDTO
        ChatRequestDTO chatRequestDTO = ChatRequestDTO.fromStringTuples(model, conversationHistory);

        // 调用 OpenAI API
        ChatResponseDTO chatResponseDTO = restTemplate.postForObject(apiUrl, chatRequestDTO, ChatResponseDTO.class);

        if (chatResponseDTO == null || chatResponseDTO.getChoices() == null || chatResponseDTO.getChoices().isEmpty()) {
            throw new Exception("No response from API");
        }

        String responseContent = chatResponseDTO.getChoices().get(0).getMessage().getContent();

        // 将助手的响应添加到 Redis 会话历史
        chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(), StringEscapeUtils.escapeHtml4(responseContent));

        // 计算 Redis 和 MongoDB 中会话长度的差异
        int redisLength = chatMessagesRedisDAO.getConversationHistory(conversationId).size();
        int mongoLength = getMongoConversationLength(conversationId);
        int diff = redisLength - mongoLength;
        logger.info("Redis length: " + redisLength + ", MongoDB length: " + mongoLength + ", diff: " + diff);

        // 如果差异超过阈值，则异步更新 MongoDB
        if (Math.abs(diff) > 5) {
            executorService.submit(() -> {
                chatSyncService.updateHistoryFromRedis(conversationId);
            });
        }

        return responseContent;
    }

    private int getMongoConversationLength(String conversationId) {
        // 从 MongoDB 获取会话长度的逻辑
        // 假设 ChatMessagesMongoDAO 提供了获取会话长度的方法
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }
}
