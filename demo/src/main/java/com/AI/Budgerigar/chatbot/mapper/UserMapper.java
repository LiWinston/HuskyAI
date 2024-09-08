package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.model.AdminInfo;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.model.UserPw;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 根据用户的 UUID 获取用户信息 Retrieve user information by UUID
     * @param uuid 用户的唯一标识符 UUID
     * @return UserPw 用户密码信息
     */
    UserPw getUserByUuid(@Param("uuid") String uuid);

    /**
     * 根据用户 UUID 获取其所有的对话记录 Retrieve all conversations by user UUID
     * @param uuid 用户的唯一标识符 UUID
     * @return List<Conversation> 用户的对话记录列表
     */
    List<Conversation> getConversationsByUserUuid(@Param("uuid") String uuid);

    /**
     * 注册新用户 Register a new user
     * @param uuid 用户的唯一标识符 UUID
     * @param username 用户名
     * @param password 密码
     * @param role 用户角色
     * @return int 插入成功的行数
     */
    @Insert("INSERT INTO UserPw (uuid, username, password, role) "
            + "VALUES (#{uuid}, #{username}, #{password}, #{role})")
    int registerUser(@Param("uuid") String uuid, @Param("username") String username, @Param("password") String password,
            @Param("role") String role);

    /**
     * 注册新的管理员 Register a new admin
     * @param uuid 用户的唯一标识符 UUID
     * @param email 管理员的邮箱
     * @param verified 管理员是否验证的标志位
     * @return int 插入成功的行数
     */
    @Insert("INSERT INTO AdminInfo (uuid, admin_level, email, verified) "
            + "VALUES (#{uuid}, 0, #{email}, #{verified})")
    int registerAdmin(@Param("uuid") String uuid, @Param("username") String username,
            @Param("password") String password, @Param("email") String email, @Param("verified") boolean verified);

    /**
     * 根据用户 UUID 提升其为管理员 Promote a user to admin by UUID
     * @param uuid 用户的唯一标识符 UUID
     */
    @Update("UPDATE UserPw SET role = 'admin' WHERE uuid = #{uuid}")
    void promoteToAdminByUuid(String uuid);

    /**
     * 根据用户名获取用户信息 Retrieve user information by username
     * @param username 用户名
     * @return UserPw 用户密码信息
     */
    @Select("SELECT uuid, username, password, role FROM UserPw WHERE username = #{username}")
    @Results({ @Result(property = "uuid", column = "uuid"), @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"), @Result(property = "role", column = "role") })
    UserPw getUserByUsername(@Param("username") String username);

    /**
     * 根据 UUID 和对话 ID 删除特定的对话记录 Delete a specific conversation by UUID and conversation ID
     * @param uuid 用户的唯一标识符 UUID
     * @param conversationId 对话的唯一标识符
     * @return int 删除的行数
     */
    int deleteConversationByUuidCid(String uuid, String conversationId);

    /**
     * 将管理员降级为普通用户 Downgrade an admin to a regular user by UUID
     * @param uuid 用户的唯一标识符 UUID
     */
    void downgradeAdminByUuid(String uuid);

    /**
     * 确认管理员身份 (验证邮箱) Confirm admin by setting the verified flag to true
     * @param uuid 管理员的唯一标识符 UUID
     */
    @Update("UPDATE AdminInfo SET verified = TRUE WHERE uuid = #{uuid}")
    void confirmAdmin(String uuid);

    /**
     * 根据 UUID 获取管理员信息 Retrieve admin information by UUID
     * @param token 管理员的唯一标识符 (token)
     * @return AdminInfo 管理员信息
     */
    @Select("SELECT * FROM AdminInfo WHERE uuid = #{uuid}")
    AdminInfo getAdminInfoByUuid(String token);

}
