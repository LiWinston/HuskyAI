package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;

import java.util.List;

public interface userService {

    Result<?> checkUserExistsByUuid(String uuid);

    List<Conversation> getConversations(String uuid);

    Result<?> register(UserRegisterDTO userRegisterDTO);

}
