package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.Entity.AdminInfo;
import com.AI.Budgerigar.chatbot.Entity.Conversation;
import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserPw> {

    List<UserPw> selectAll();

    /**
     * Retrieve user information by UUID
     * @param uuid user's only identifier UUID
     * @return UserPw user password information
     */
    UserPw getUserByUuid(@Param("uuid") String uuid);

    /**
     * Retrieve all conversations by user UUID
     * @param uuid user's only identifier UUID
     * @return List<Conversation> user's conversation list
     */
    List<Conversation> getConversationsByUserUuid(@Param("uuid") String uuid);

    /**
     * Register a new user
     * @param uuid user's only identifier UUID
     * @param username
     * @param password
     * @param role
     * @return int Number of rows successfully inserted.
     */
    @Insert("INSERT INTO UserPw (uuid, username, password, role) "
            + "VALUES (#{uuid}, #{username}, #{password}, #{role})")
    int registerUser(@Param("uuid") String uuid, @Param("username") String username, @Param("password") String password,
            @Param("role") String role);

    /**
     * Register a new admin
     * @param uuid user's only identifier UUID
     * @param email
     * @param verified Flag indicating whether the administrator is verified.
     * @return int Number of rows successfully inserted.
     */
    @Insert("INSERT INTO AdminInfo (uuid, admin_level, email, verified) "
            + "VALUES (#{uuid}, 0, #{email}, #{verified})")
    int registerAdmin(@Param("uuid") String uuid, @Param("username") String username,
            @Param("password") String password, @Param("email") String email, @Param("verified") boolean verified);

    /**
     * Promote a user to admin by UUID
     * @param uuid user's only identifier UUID
     */
    void promoteToAdminByUuid(String uuid);

    /**
     * Retrieve user information by username
     * @param username
     * @return UserPw user password information
     */
    @Select("SELECT uuid, username, password, role FROM UserPw WHERE username = #{username}")
    @Results({ @Result(property = "uuid", column = "uuid"), @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"), @Result(property = "role", column = "role") })
    UserPw getUserByUsername(@Param("username") String username);

    /**
     * Delete a specific conversation by UUID and conversation ID
     * @param uuid user's only identifier UUID
     * @param conversationId conversation only identifier
     * @return int Number of rows deleted.
     */
    int deleteConversationByUuidCid(String uuid, String conversationId);

    /**
     * Downgrade an admin to a regular user by UUID
     * @param uuid user's only identifier UUID
     */
    void downgradeAdminByUuid(String uuid);

    /**
     * Confirm admin by setting the verified flag to true
     * @param uuid user's only identifier UUID
     */
    @Update("UPDATE AdminInfo SET verified = TRUE WHERE uuid = #{uuid}")
    void confirmAdmin(String uuid);

    /**
     * Retrieve admin information by UUID
     * @param uuid The administrator's unique identifier
     * @return AdminInfo admin information
     */
    @Select("SELECT uuid, admin_level as adminLevel, email, verified FROM AdminInfo WHERE uuid = #{uuid}")
    AdminInfo getAdminInfoByUuid(String uuid);

    /**
     * Update existing admin information
     * @param uuid user's only identifier UUID
     * @param email admin's email
     * @param adminLevel admin's level
     * @return int Number of rows updated
     */
    @Update("UPDATE AdminInfo SET email = #{email}, admin_level = #{adminLevel} WHERE uuid = #{uuid}")
    int updateAdminInfo(@Param("uuid") String uuid, @Param("email") String email, @Param("adminLevel") int adminLevel);

    /**
     * Create a new admin from admin dashboard
     * @param uuid user's only identifier UUID
     * @param email admin's email
     * @param adminLevel admin's level
     * @param verified Flag indicating whether the administrator is verified
     * @return int Number of rows successfully inserted
     */
    @Insert("INSERT INTO AdminInfo (uuid, admin_level, email, verified) VALUES (#{uuid}, #{adminLevel}, #{email}, #{verified})")
    int createAdminFromDashboard(@Param("uuid") String uuid, @Param("email") String email, 
            @Param("adminLevel") int adminLevel, @Param("verified") boolean verified);

    /**
     * Update only admin level
     * @param uuid user's only identifier UUID
     * @param adminLevel admin's level
     * @return int Number of rows updated
     */
    @Update("UPDATE AdminInfo SET admin_level = #{adminLevel} WHERE uuid = #{uuid}")
    int updateAdminLevel(@Param("uuid") String uuid, @Param("adminLevel") int adminLevel);

}
