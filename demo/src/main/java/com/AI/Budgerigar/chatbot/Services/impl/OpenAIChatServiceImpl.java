package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.DTO.ChatRequest;
import com.AI.Budgerigar.chatbot.DTO.ChatResponse;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

;

@Service
public class OpenAIChatServiceImpl implements ChatService {

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Override
    public String chat(String prompt) throws Exception {
        // Create request
        ChatRequest request = new ChatRequest(model, prompt);

        // Invoke API
        ChatResponse response = restTemplate.postForObject(apiUrl, request, ChatResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new Exception("No response from API");
        }

        return response.getChoices().get(0).getMessage().getContent();
    }
}
