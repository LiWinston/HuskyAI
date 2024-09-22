package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.result.Result;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionResult;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class DouBaoChatServiceImpl implements ChatService {

    @Setter
    @Getter
    public String conversationId;

    private final ArkService arkService;

    @Value("${doubao.model.id:ep-20240823074926-tvjgz}")
    private String model;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Autowired
    @Qualifier("chatMessagesMongoDAOImpl")
    private ChatMessagesMongoDAO chatMessagesMongoDAO;

    @Autowired
    private ChatSyncServiceImpl chatSyncService;

    @Autowired
    private TokenLimiter tokenLimiter;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    DateTimeFormatter dateTimeFormatter;

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @Autowired
    public DouBaoChatServiceImpl(ArkService arkService) {
        this.arkService = arkService;
    }

    @PostConstruct
    public void init() {
        conversationId = "default_doubao_conversation";
    }

    public List<String[]> getHistoryPreChat(String prompt, String conversationId) {
        chatSyncService.updateRedisFromMongo(conversationId);

        chatMessagesRedisDAO.maintainMessageHistory(conversationId);

        // 添加用户输入到 Redis 对话历史
        chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(), prompt);

        // 从 Redis 中获取对话历史
        List<String[]> conversationHistory;
        try {
            conversationHistory = tokenLimiter.getAdaptiveConversationHistory(conversationId, 16000);
            log.info("自适应缩放到" + conversationHistory.size() + "条消息");
            // for (String[] entry : conversationHistory) {
            // log.info("{} : {}", entry[0], entry[2].substring(0, Math.min(20,
            // entry[2].length())));
            // }
            return conversationHistory;
        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", TokenLimiter.class.getName(), e.getMessage(), e);
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    "Query failed. Please try again.");
            throw new RuntimeException("Error processing chat request", e);
        }
    }

    @SneakyThrows
    @Override
    public Result<String> chat(String prompt, String conversationId) {
        List<String[]> conversationHistory = getHistoryPreChat(prompt, conversationId);

        // Build ChatMessage list from conversation history
        List<ChatMessage> messages = new java.util.ArrayList<>(conversationHistory.stream()
            .map(entry -> ChatMessage.builder()
                .role(entry[0].equals("user") ? ChatMessageRole.USER : ChatMessageRole.ASSISTANT)
                .content(entry[2])
                .build())
            .toList());

        // Create ChatCompletionRequest with full conversation history
        ChatCompletionRequest request = ChatCompletionRequest.builder().model(model).messages(messages).build();

        // Invoke API
        ChatCompletionResult result = arkService.createChatCompletion(request);

        if (result != null && result.getChoices() != null && !result.getChoices().isEmpty()) {
            String responseContent = (String) result.getChoices().get(0).getMessage().getContent();

            logInfo(responseContent);
            // Add assistant response to Redis conversation history
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(), responseContent);

            // Calculate the difference in conversation length
            int redisLength = chatMessagesRedisDAO.getConversationHistory(conversationId).size();
            int mongoLength = getMongoConversationLength(conversationId);
            int diff = redisLength - mongoLength;

            // If difference exceeds threshold, update MongoDB asynchronously
            if (Math.abs(diff) > 5) {
                executorService.submit(() -> {
                    chatSyncService.updateHistoryFromRedis(conversationId);
                });
            }

            return Result.success(responseContent);
        }
        else {
            throw new Exception("No response from API");
        }
    }

    private int getMongoConversationLength(String conversationId) {
        // Implement the logic to get the conversation length from MongoDB
        // Assuming ChatMessagesMongoDAO has a method to get the conversation length
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }

}