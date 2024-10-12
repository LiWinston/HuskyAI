package com.AI.Budgerigar.chatbot.chatbotAdmin.Controller;


import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.es.ChatConversationAggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/chat-conversations")
public class ChatConversationController {

    @Autowired
    private ChatConversationSearchService searchService;

    @Autowired
    private ChatConversationAggregationService aggregationService;

    // 搜索对话片段并返回封装后的 Message 列表
    @GetMapping("/search")
    public List<Message> searchConversations(@RequestParam String keyword) {
        return searchService.searchByKeyword(keyword);
    }

    // 获取前 n 个热搜关键词
    @GetMapping("/top-keywords")
    public List<String> getTopKeywords(@RequestParam int n) throws IOException {
        return aggregationService.getTopKeywords(n);
    }
}