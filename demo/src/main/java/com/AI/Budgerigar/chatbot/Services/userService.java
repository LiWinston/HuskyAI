package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.model.Cid;

import java.util.List;

public interface userService {
    boolean checkUserExists(String uuid);

    List<Cid> getConversations(String uuid);
}
