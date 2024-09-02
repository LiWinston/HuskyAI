package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements userService {

    @Autowired
    private UserMapper userMapper;

//    public Result<?> login(String username, String password) {
//        UserPw user = userMapper.getUserByUsername(username);
//        if (user == null) {
//            return Result.error("User not found");
//        }
//        if (!user.getPassword().equals(password)) {
//            return Result.error("Password incorrect");
//        }
//
//        //查询用户是否开启二步验证
//        //check if user has enabled two-step verification
//        if (user.isMFA() == 1) {
//            //返回相应二步验证服务的URL
//            //return the URL of the corresponding two-step verification service
//            return Result.error("Two-step verification required");
//        }
//        return Result.success(user.getUuid());
//    }

    @Override
    public Result<Boolean> checkUserExistsByUuid(String uuid) {
        try{
            UserPw user = userMapper.getUserByUuid(uuid);
            if(user == null){
                return Result.error("User not found");
            }
            return Result.success(true, "UserName: " + user.getUsername());
        }catch (Exception e){
            return Result.error(e.getMessage());
        }
//        UserPw user = userMapper.getUserByUuid(uuid);

    }

    @Override
    public List<Conversation> getConversations(String uuid) {
        return userMapper.getConversationsByUserUuid(uuid);
    }
}
