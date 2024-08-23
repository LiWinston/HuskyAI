package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.DTO.ErrorResponse;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionResult;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatController {

    @Autowired
    private ArkService arkService;

//    @Value("${volcengine.model}")
    @Value("ep-20240823074926-tvjgz")
    private String model;

    @GetMapping("/chat")
    public ResponseEntity<?> chat(@RequestParam String prompt) {
        try {
            // create ChatCompletionRequest
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            ChatMessage.builder().role(ChatMessageRole.SYSTEM).content("你是豆包，是由字节跳动开发的 AI 人工智能助手").build(),
                            ChatMessage.builder().role(ChatMessageRole.USER).content(prompt).build()
                    ))
                    .build();

            // invoke API
            ChatCompletionResult result = arkService.createChatCompletion(request);

            // extract the content from the first choice's message
            if (result != null && result.getChoices() != null && !result.getChoices().isEmpty()) {
                String content = (String) result.getChoices().get(0).getMessage().getContent();
                return ResponseEntity.ok(content);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("No response from API"));
            }
        } catch (Exception e) {
            // catch any exception and return error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }
}
