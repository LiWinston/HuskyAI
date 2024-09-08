package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.model.AdminInfo;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.model.UserPw;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    UserPw getUserByUuid(@Param("uuid") String uuid);

    List<Conversation> getConversationsByUserUuid(@Param("uuid") String uuid);

    @Insert("INSERT INTO UserPw (uuid, username, password, role) "
            + "VALUES (#{uuid}, #{username}, #{password}, #{role})")
    int registerUser(@Param("uuid") String uuid, @Param("username") String username, @Param("password") String password,
            @Param("role") String role);

    @Insert("INSERT INTO AdminInfo (uuid, admin_level, email, verified) "
            + "VALUES (#{uuid}, 0, #{email}, #{verified})")
    int registerAdmin(@Param("uuid") String uuid, @Param("username") String username,
            @Param("password") String password, @Param("email") String email, @Param("verified") boolean verified);

    @Update("UPDATE UserPw SET role = 'admin' WHERE uuid = #{uuid}")
    void promoteToAdminByUuid(String uuid);

    @Select("SELECT uuid, username, password, role FROM UserPw WHERE username = #{username}")
    @Results({ @Result(property = "uuid", column = "uuid"), @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"), @Result(property = "role", column = "role") })
    UserPw getUserByUsername(@Param("username") String username);

    int deleteConversationByUuidCid(String uuid, String conversationId);

    @Update("UPDATE AdminInfo SET verified = TRUE WHERE uuid = #{uuid}")
    void confirmAdmin(String uuid);

    @Select("SELECT * FROM AdminInfo WHERE uuid = #{uuid}")
    AdminInfo getAdminInfoByUuid(String token);

}