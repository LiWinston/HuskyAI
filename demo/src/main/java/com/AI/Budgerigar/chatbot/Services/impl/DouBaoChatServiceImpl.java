package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionResult;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DouBaoChatServiceImpl implements ChatService {

    private final ArkService arkService;

    @Value("ep-20240823074926-tvjgz") // Or use @Value for the property
    private String model;

    @Autowired
    public DouBaoChatServiceImpl(ArkService arkService) {
        this.arkService = arkService;
    }

    @Override
    public String chat(String prompt) throws Exception {
        // Create ChatCompletionRequest
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                        ChatMessage.builder().role(ChatMessageRole.SYSTEM).content("你是豆包，是由字节跳动开发的 AI 人工智能助手").build(),
                        ChatMessage.builder().role(ChatMessageRole.USER).content(prompt).build()
                ))
                .build();

        // Invoke API
        ChatCompletionResult result = arkService.createChatCompletion(request);

        if (result != null && result.getChoices() != null && !result.getChoices().isEmpty()) {
            return (String) result.getChoices().get(0).getMessage().getContent();
        } else {
            throw new Exception("No response from API");
        }
    }
}
