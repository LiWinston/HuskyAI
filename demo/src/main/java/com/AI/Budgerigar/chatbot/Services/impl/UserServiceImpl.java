package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Cache.AdminWaitingListRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.Services.EmailService;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.Entity.AdminInfo;
import com.AI.Budgerigar.chatbot.Entity.Conversation;
import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements userService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("resendEmailService")
    private EmailService emailService;

    @Autowired
    private Environment env;

    @Autowired
    private AdminWaitingListRedisDAO adminWaitingListRedisDAO;

    @Override
    public Result<Boolean> checkUserExistsByUuid(String uuid) {
        try {
            UserPw user = userMapper.getUserByUuid(uuid);
            if (user == null) {
                return Result.error("User not found");
            }
            return Result.success(true, "UserName: " + user.getUsername());
        }
        catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<Boolean> checkUserIsAdminByUuid(String uuid) {
        try {
            AdminInfo adminInfo = userMapper.getAdminInfoByUuid(uuid);
            if (adminInfo == null) {
                return Result.error(false, "Not admin.");
            }
            if (!adminInfo.isVerified()) {
                return Result.error(false, "Admin not verified.");
            }
            return Result.success(true, "Admin verified.");
        }
        catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public List<Conversation> getConversations(String uuid) {
        return userMapper.getConversationsByUserUuid(uuid);
    }

    @Override
    @Transactional
    public Result<?> register(HttpServletRequest request, UserRegisterDTO userRegisterDTO) {
        log.info(userRegisterDTO.toString());
        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();
        String adminEmail = userRegisterDTO.getAdminEmail();

        try {
            if (userMapper.getUserByUsername(username) != null) {
                return Result.error("Username already exists.");
            }

            String uuid = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

            if (!userRegisterDTO.getIsAdmin()) {
                userMapper.registerUser(uuid, username, encodedPassword, "USER");
                return Result.success(null, "User registered successfully.");
            }
            else {
                if (adminEmail == null || adminEmail.isEmpty()) {
                    return Result.error("Admin email required.");
                }

                try {
                    String inviteToken = UUID.randomUUID().toString();
                    adminWaitingListRedisDAO.addAdminToWaitingList(inviteToken, uuid);
                    log.info("Invite tk for" + username + ": " + inviteToken);

                    // String serverPort = env.getProperty("server.port", "3000");
                    String confirmationLink = request.getScheme() + "://" + request.getServerName()
                            + (request.getServerName().equals("localhost") ? ":" + 3000 : "")
                            + "/user/register/confirm/" + inviteToken;
                    String emailContent = "<p>Please confirm your admin registration by clicking the following link:</p>"
                            + "<p><a href='" + confirmationLink + "'>" + confirmationLink + "</a></p>";

                    emailService.sendEmail(adminEmail, "Admin Registration Confirmation", emailContent);
                }
                catch (Exception e) {
                    log.error("Failed to send confirmation email.", e);
                    return Result.error("Failed to send confirmation email.");
                }

                userMapper.registerUser(uuid, username, encodedPassword, "USER");
                userMapper.registerAdmin(uuid, username, encodedPassword, adminEmail, false);
                return Result.success(null, "请查看您的邮箱以确认管理员注册，但您可以随时登录以聊天。");
            }
        }
        catch (Exception e) {
            log.error("User registration failed.", e);
            return Result.error("User registration failed.");
        }
    }

    @Override
    @Transactional
    public Result<?> confirmAdmin(String token) {
        try {
            String uuid = adminWaitingListRedisDAO.getUuidByToken(token);
            if (uuid == null) {
                return Result.error("Link expired.");
            }
            UserPw user = userMapper.getUserByUuid(uuid);
            if (user == null) {
                return Result.error("Not registered or link expired.");
            }

            AdminInfo adminInfo = userMapper.getAdminInfoByUuid(user.getUuid());
            if (adminInfo == null) {
                userMapper.downgradeAdminByUuid(user.getUuid());
                return Result.error("Apply for admin first or link expired.");
            }
            if (adminInfo.isVerified()) {
                return Result.error("Admin is already verified.");
            }

            userMapper.confirmAdmin(user.getUuid());
            userMapper.promoteToAdminByUuid(user.getUuid());
            adminWaitingListRedisDAO.removeToken(token);
            return Result.success(user.getUsername(), "Admin registration confirmed.");
        }
        catch (Exception e) {
            log.error("Admin registration confirmation failed.", e);
            return Result.error("Admin registration confirmation failed.");
        }
    }

    @Override
    public Page<Conversation> getConversationsWithPage(String uuid, Page<Conversation> page) {
        log.info("开始分页查询对话列表: uuid={}, current={}, size={}", 
                uuid, page.getCurrent(), page.getSize());
        try {
            IPage<Conversation> result = userMapper.getConversationsByUserUuidWithPage(page, uuid);
            if(result == null) {
                log.error("分页查询结果为空");
                return new Page<>();
            }
            log.info("分页查询成功: total={}, pages={}, current={}, size={}", 
                    result.getTotal(), result.getPages(), 
                    result.getCurrent(), result.getSize());
            return (Page<Conversation>) result;
        } catch (Exception e) {
            log.error("分页查询异常", e);
            return new Page<>();
        }
    }

}
