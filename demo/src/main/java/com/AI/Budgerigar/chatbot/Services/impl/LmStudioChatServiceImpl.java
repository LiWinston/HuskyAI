package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.result.Result;

import java.util.List;

public class LmStudioChatServiceImpl implements ChatService {

    @Override
    public List<String[]> getHistoryPreChat(String prompt, String conversationId) throws Exception {
        return List.of();
    }

    @Override
    public Result<String> chat(String prompt, String conversationId) {
        return null;
    }

    @Override
    public void logInfo(String message) {
        ChatService.super.logInfo(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        ChatService.super.logError(message, throwable);
    }

    @Override
    public String getCallingClassName() {
        return ChatService.super.getCallingClassName();
    }

}
