package com.AI.Budgerigar.chatbot.Mapper;

import com.AI.Budgerigar.chatbot.Entity.UserPw;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserPwMapper {

    @Select("SELECT * FROM user_pw WHERE username = #{username}")
    UserPw findByUsername(String username);

    @Select("SELECT * FROM user_pw WHERE sso_id = #{ssoId}")
    UserPw findBySSOId(String ssoId);

    @Insert("INSERT INTO user_pw (uuid, username, password, email, sso_id) VALUES (#{uuid}, #{username}, #{password}, #{email}, #{ssoId})")
    void insert(UserPw user);

    @Select("SELECT * FROM user_pw WHERE uuid = #{uuid}")
    UserPw findByUuid(String uuid);

    @Update("UPDATE user_pw SET sso_id = #{ssoId} WHERE uuid = #{uuid}")
    void updateSSOId(@Param("uuid") String uuid, @Param("ssoId") String ssoId);
} 