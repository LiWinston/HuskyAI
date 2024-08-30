package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.ChatRequest;
import com.AI.Budgerigar.chatbot.DTO.ChatResponse;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OpenAIChatServiceImpl implements ChatService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Autowired
    private ChatMessagesMongoDAO chatMessagesMongoDAO;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Override
    public String chat(String prompt) throws Exception {
        String conversationId = "openai_conversation_id"; // 实际应用中应生成唯一会话ID

        // 从 Redis 获取完整的会话历史
        List<String[]> conversationHistory = chatMessagesRedisDAO.getConversationHistory(conversationId);

        // 将用户的输入添加到 Redis 会话历史
        chatMessagesRedisDAO.addMessage(conversationId, "user", prompt);

        // 使用工厂方法从 String[] 列表创建 ChatRequest
        ChatRequest chatRequest = ChatRequest.fromStringPairs(model, conversationHistory);

        // 调用 OpenAI API
        ChatResponse chatResponse = restTemplate.postForObject(apiUrl, chatRequest, ChatResponse.class);

        if (chatResponse == null || chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) {
            throw new Exception("No response from API");
        }

        String responseContent = chatResponse.getChoices().get(0).getMessage().getContent();

        // 将助手的响应添加到 Redis 会话历史
        chatMessagesRedisDAO.addMessage(conversationId, "assistant", responseContent);

        // 计算 Redis 和 MongoDB 中会话长度的差异
        int redisLength = chatMessagesRedisDAO.getConversationHistory(conversationId).size();
        int mongoLength = getMongoConversationLength(conversationId);
        int diff = redisLength - mongoLength;

        // 如果差异超过阈值，则异步更新 MongoDB
        if (diff > 5) {
            executorService.submit(() -> chatMessagesMongoDAO.updateHistoryById(conversationId, diff));
        }

        return responseContent;
    }

    private int getMongoConversationLength(String conversationId) {
        // 从 MongoDB 获取会话长度的逻辑
        // 假设 ChatMessagesMongoDAO 提供了获取会话长度的方法
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }
}
