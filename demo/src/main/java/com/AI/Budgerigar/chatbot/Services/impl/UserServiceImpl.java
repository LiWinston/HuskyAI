package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.model.UserPw;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;

import java.util.List;

@Service
public class UserServiceImpl implements userService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean checkUserExists(String uuid) {
        UserPw user = userMapper.getUserByUuid(uuid);
        return user != null;
    }

    @Override
    public List<Conversation> getConversations(String uuid) {
        return userMapper.getConversationsByUserUuid(uuid);
    }
}
