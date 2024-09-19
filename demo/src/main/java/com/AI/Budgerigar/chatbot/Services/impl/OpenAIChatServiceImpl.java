package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.ChatRequestDTO;
import com.AI.Budgerigar.chatbot.DTO.ChatResponseDTO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.result.Result;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@NoArgsConstructor
public class OpenAIChatServiceImpl implements ChatService {

    @Autowired
    private ExecutorService executorService;

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

    @Autowired
    private TokenLimiter tokenLimiter;

    @Getter
    @Setter
    @Value("${openai.model:${PC.LMStudioServer.model}}")
    private String model;

    @Getter
    @Setter
    @Value("${openai.api.url:${PC.LMStudioServer.url}}")
    private String openAIUrl;

    public OpenAIChatServiceImpl(String openAIUrl, String model) {
        this.model = model;
        this.openAIUrl = openAIUrl;
    }

    public static OpenAIChatServiceImpl create(String openAIUrl, String model) {
        return new OpenAIChatServiceImpl(openAIUrl, model);
    }

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @Override
    public Result<String> chat(String prompt, String conversationId) {
        try {
            chatSyncService.updateRedisFromMongo(conversationId);

            chatMessagesRedisDAO.maintainMessageHistory(conversationId);

            // 添加用户输入到 Redis 对话历史
            chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(),
                    StringEscapeUtils.escapeHtml4(prompt));

            // 从 Redis 中获取对话历史
            List<String[]> conversationHistory = null;
            try {
                conversationHistory = tokenLimiter.getAdaptiveConversationHistory(conversationId, 16000);
                log.info("自适应缩放到" + conversationHistory.size() + "条消息");
                // for (String[] entry : conversationHistory) {
                // log.info("{} : {}", entry[0], entry[2].substring(0, Math.min(20,
                // entry[2].length())));
                // }
            }
            catch (Exception e) {
                log.error("Error occurred in {}: {}", TokenLimiter.class.getName(), e.getMessage(), e);
                chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                        "Query failed. Please try again.");
                throw new RuntimeException("Error processing chat request", e);
            }

            // 使用工厂方法从 String[] 列表创建 ChatRequestDTO
            ChatRequestDTO chatRequestDTO = ChatRequestDTO.fromStringTuples(model, conversationHistory);

            ChatResponseDTO chatResponseDTO = restTemplate.postForObject(openAIUrl, chatRequestDTO,
                    ChatResponseDTO.class);

            if (chatResponseDTO == null || chatResponseDTO.getChoices() == null
                    || chatResponseDTO.getChoices().isEmpty()) {
                throw new Exception("No response from API");
            }

            String result = chatResponseDTO.getChoices().get(0).getMessage().getContent();

            log.info("Response from OpenAI: {}", result.substring(0, Math.min(40, result.length())));

            // 将助手的响应添加到 Redis 会话历史
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    // StringEscapeUtils.escapeHtml4(result));
                    result);
            // 计算 Redis 和 MongoDB 中会话长度的差异
            int redisLength = chatMessagesRedisDAO.getConversationHistory(conversationId).size();
            int mongoLength = getMongoConversationLength(conversationId);
            int diff = redisLength - mongoLength;
            log.info("Redis length: {}, MongoDB length: {}, diff: {} FROM {}", redisLength, mongoLength, diff,
                    OpenAIChatServiceImpl.class.getName());

            // 如果差异超过阈值，则异步更新 MongoDB
            if (Math.abs(diff) > 5) {
                executorService.submit(() -> {
                    chatSyncService.updateHistoryFromRedis(conversationId);
                });
            }

            return Result.success(result,
                    "From " + chatResponseDTO.getModel() + ", Referenced " + conversationHistory.size() + " messages.");
        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", OpenAIChatServiceImpl.class.getName(), e.getMessage());
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    "Query failed. Please try again.");
            throw new RuntimeException("Error processing chat request", e);
        }
    }

    private int getMongoConversationLength(String conversationId) {
        // 从 MongoDB 获取会话长度的逻辑
        // 假设 ChatMessagesMongoDAO 提供了获取会话长度的方法
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }

}
