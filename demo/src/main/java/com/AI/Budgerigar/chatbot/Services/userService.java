package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface userService {

    Result<?> checkUserExistsByUuid(String uuid);

    List<Conversation> getConversations(String uuid);

    Result<?> register(HttpServletRequest request, UserRegisterDTO userRegisterDTO);

    Result<?> confirmAdmin(String token);

    Result<Boolean> checkUserIsAdminByUuid(String uuid);

}
