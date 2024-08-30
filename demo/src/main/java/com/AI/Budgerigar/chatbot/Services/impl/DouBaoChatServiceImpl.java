package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionResult;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
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

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    public DouBaoChatServiceImpl(ArkService arkService) {
        this.arkService = arkService;
    }

    @SneakyThrows
    @Override
    public String chat(String prompt) {
        // Use a fixed or consistent conversationId to ensure continuity across messages
        conversationId = "default_doubao_conversation"; // Replace with a proper session/user ID in a real-world scenario

        // Retrieve full conversation history from Redis
        List<String[]> conversationHistory = chatMessagesRedisDAO.getConversationHistory(conversationId);

        // Add user prompt to Redis conversation history
        chatMessagesRedisDAO.addMessage(conversationId, "user", prompt);

        // Build ChatMessage list from conversation history
        List<ChatMessage> messages = new java.util.ArrayList<>(conversationHistory.stream()
                .map(entry -> ChatMessage.builder()
                        .role(entry[0].equals("user") ? ChatMessageRole.USER : ChatMessageRole.ASSISTANT)
                        .content(entry[1])
                        .build())
                .toList());

        // Add the current prompt as the latest message
        messages.add(ChatMessage.builder().role(ChatMessageRole.USER).content(prompt).build());

        // Create ChatCompletionRequest with full conversation history
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .build();

        // Invoke API
        ChatCompletionResult result = arkService.createChatCompletion(request);

        if (result != null && result.getChoices() != null && !result.getChoices().isEmpty()) {
            String responseContent = (String) result.getChoices().get(0).getMessage().getContent();

            logInfo(responseContent);
            // Add assistant response to Redis conversation history
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", responseContent);

            // Calculate the difference in conversation length
            int redisLength = chatMessagesRedisDAO.getConversationHistory(conversationId).size();
            int mongoLength = getMongoConversationLength(conversationId);
            int diff = redisLength - mongoLength;

            // If difference exceeds threshold, update MongoDB asynchronously
            if (diff > 5) {
                executorService.submit(() -> chatMessagesMongoDAO.updateHistoryById(conversationId, diff));
            }

            return responseContent;
        } else {
            throw new Exception("No response from API");
        }
    }

    private int getMongoConversationLength(String conversationId) {
        // Implement the logic to get the conversation length from MongoDB
        // Assuming ChatMessagesMongoDAO has a method to get the conversation length
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }
}