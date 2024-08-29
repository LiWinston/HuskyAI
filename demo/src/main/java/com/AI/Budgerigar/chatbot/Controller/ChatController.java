package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.DTO.ErrorResponse;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    @Qualifier("baidu")
    private ChatService chatService;


    @GetMapping("/chat")
    public ResponseEntity<?> chat(@RequestParam String prompt) {
        try {
            // Use chatService to handle the request
            String response = chatService.chat(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Catch any exception and return error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> chatPost(@RequestBody Map<String, String> body) {
        try {
            // Use chatService to handle the request
            String response = chatService.chat(body.get("prompt"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Catch any exception and return error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }
}
