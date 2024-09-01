package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.model.Conversation;

import java.util.List;

public interface userService {
    boolean checkUserExists(String uuid);

    List<Conversation> getConversations(String uuid);
}
