package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.DTO.PageDTO;
import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.Entity.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface userService {

    Result<?> checkUserExistsByUuid(String uuid);

    List<Conversation> getConversations(String uuid);

    Result<?> register(HttpServletRequest request, UserRegisterDTO userRegisterDTO);

    Result<?> confirmAdmin(String token);

    Result<Boolean> checkUserIsAdminByUuid(String uuid);

    /**
     * 分页获取用户的对话列表
     * @param uuid 用户UUID
     * @param pageDTO 分页参数
     * @return 分页后的对话列表
     */
    PageInfo<Conversation> getConversationsWithPage(String uuid, PageDTO pageDTO);

}
