package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.model.UserPw;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    UserPw getUserByUuid(@Param("uuid") String uuid);

    List<Conversation> getConversationsByUserUuid(@Param("uuid") String uuid);

    int registerUser(@Param("uuid") String uuid, @Param("username") String username, @Param("password") String password);

    UserPw getUserByUsername(@Param("username") String username);

}