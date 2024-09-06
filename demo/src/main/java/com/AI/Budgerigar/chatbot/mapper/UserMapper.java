package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.model.UserPw;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    UserPw getUserByUuid(@Param("uuid") String uuid);

    List<Conversation> getConversationsByUserUuid(@Param("uuid") String uuid);

    @Insert("INSERT INTO UserPw (uuid, username, password, role) " +
            "VALUES (#{uuid}, #{username}, #{password}, 'USER')")
    int registerUser(@Param("uuid") String uuid, @Param("username") String username, @Param("password") String password);

    @Select("SELECT uuid, username, password, role FROM UserPw WHERE username = #{username}")
    @Results({
            @Result(property = "uuid", column = "uuid"),
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "role", column = "role")
    })
    UserPw getUserByUsername(@Param("username") String username);


}