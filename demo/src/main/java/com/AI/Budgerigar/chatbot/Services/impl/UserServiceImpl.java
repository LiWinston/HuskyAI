package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Cache.AdminWaitingListRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.Entity.AdminInfo;
import com.AI.Budgerigar.chatbot.Entity.Conversation;
import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceImpl implements userService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

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
        // UserPw user = userMapper.getUserByUuid(uuid);

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
        String adminEmail = userRegisterDTO.getAdminEmail(); // Get the admin email, null
                                                             // if not registered as an
                                                             // admin.

        try {
            // Check if the username already exists.
            if (userMapper.getUserByUsername(username) != null) {
                return Result.error("Username already exists.");
            }

            String uuid = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

            // Register as a regular user.
            if (!userRegisterDTO.getIsAdmin()) {
                userMapper.registerUser(uuid, username, encodedPassword, "USER");
                return Result.success(null, "User registered successfully.");
            }
            // Processing administrator registration.
            else {
                if (adminEmail == null || adminEmail.isEmpty()) {
                    return Result.error("Admin email required.");
                }
                //
                // if (!adminEmail.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                // return Result.error("Invalid email format.");
                // }
                //
                try {
                    // Generate a complex confirmation link token.
                    String inviteToken = UUID.randomUUID().toString();

                    // Store the token and uuid in Redis using DAO.
                    adminWaitingListRedisDAO.addAdminToWaitingList(inviteToken, uuid);

                    log.info("Invite tk for" + username + ": " + inviteToken);

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(env.getProperty("spring.mail.username"));
                    message.setTo(adminEmail);
                    message.setSubject("Admin Registration Confirmation");
                    message.setText("Please confirm your admin registration by clicking the following link: "
                            + request.getScheme() + "://" + request.getServerName()
                            // + ":" + request.getServerPort()
                            + "/user/register/confirm/" + inviteToken);
                    mailSender.send(message);
                }
                catch (MailException e) {
                    log.error("Failed to send confirmation email.", e);
                    return Result.error("Failed to send confirmation email.");
                }

                // Mark this administrator as "unconfirmed" in the database.
                userMapper.registerUser(uuid, username, encodedPassword, "USER");
                userMapper.registerAdmin(uuid, username, encodedPassword, adminEmail, false);
                return Result.success(null, "Please confirm your registration via email.");
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
            // Retrieve user information from the database.
            UserPw user = userMapper.getUserByUuid(uuid);
            if (user == null) {
                return Result.error("Not registered or link expired.");
            }

            // Obtain administrator information and check if it has been verified.
            AdminInfo adminInfo = userMapper.getAdminInfoByUuid(user.getUuid());
            if (adminInfo == null) {
                userMapper.downgradeAdminByUuid(user.getUuid());
                return Result.error("Apply for admin first or link expired.");
            }
            if (adminInfo.isVerified()) {
                return Result.error("Admin is already verified.");
            }

            // Update admin status to verified.
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

}
