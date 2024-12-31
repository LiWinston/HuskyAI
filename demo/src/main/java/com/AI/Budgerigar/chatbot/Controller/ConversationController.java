package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat/conversation")
@Slf4j
public class ConversationController {

    @Autowired
    private ConversationMapper conversationMapper;

    // Get conversation title
    @GetMapping("/{conversationId}/title")
    public Result<String> getConversationTitle(@PathVariable String conversationId) {
        try {
            String title = conversationMapper.getSummaryByCid(conversationId);
            return Result.success(title != null ? title : "未命名对话");
        } catch (Exception e) {
            log.error("Failed to get conversation title: ", e);
            return Result.error("Failed to get conversation title");
        }
    }
} 