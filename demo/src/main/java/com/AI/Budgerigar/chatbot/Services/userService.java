package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.Entity.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface userService {

    Result<?> checkUserExistsByUuid(String uuid);

    List<Conversation> getConversations(String uuid);

    Result<?> register(HttpServletRequest request, UserRegisterDTO userRegisterDTO);

    Result<?> confirmAdmin(String token);

    Result<Boolean> checkUserIsAdminByUuid(String uuid);

    Page<Conversation> getConversationsWithPage(String uuid, Page<Conversation> page);

}
