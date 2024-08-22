package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.DTO.ChatRequest;
import com.AI.Budgerigar.chatbot.DTO.ChatResponse;
import com.AI.Budgerigar.chatbot.DTO.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ChatController {

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    @GetMapping("/chat")
    public ResponseEntity<?> chat(@RequestParam String prompt) {
//        return ResponseEntity.ok("Hello, World!");
        try {
            // create request
            ChatRequest request = new ChatRequest(model, prompt);

            // invoke API
            ChatResponse response = restTemplate.postForObject(apiUrl, request, ChatResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("No response from API"));
            }

            // return response at index 0
            return ResponseEntity.ok(response.getChoices().get(0).getMessage().getContent());
        } catch (Exception e) {
            // catch any exception and return error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }
}
