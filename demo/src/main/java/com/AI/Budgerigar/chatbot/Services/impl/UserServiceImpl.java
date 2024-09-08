package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public List<Conversation> getConversations(String uuid) {
        return userMapper.getConversationsByUserUuid(uuid);
    }

    @Override
    public Result<?> register(UserRegisterDTO userRegisterDTO) {
        log.info(userRegisterDTO.toString());
        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();
        String adminEmail = userRegisterDTO.getAdminEmail(); // 获取管理员邮箱，如果不是管理员注册则为null

        try {
            // 检查用户名是否已存在
            if (userMapper.getUserByUsername(username) != null) {
                return Result.error("Username already exists.");
            }

            String uuid = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

            // 注册普通用户
            if (!userRegisterDTO.getIsAdmin()) {
                userMapper.registerUser(uuid, username, encodedPassword);
                return Result.success(null, "User registered successfully.");
            }
            // 处理管理员注册
            else {
                if(adminEmail == null || adminEmail.isEmpty()) {
                    return Result.error("Admin email required.");
                }
//                //进一步检查邮箱格式
//                if (!adminEmail.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
//                    return Result.error("Invalid email format.");
//                }
                // 发送确认邮件
                try{
                    log.info(mailSender.toString());
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(env.getProperty("spring.mail.username"));
                    message.setTo(adminEmail);
                    message.setSubject("Admin Registration Confirmation");
                    message.setText("Please confirm your admin registration by clicking the following link: ...");
                    mailSender.send(message);
                }catch (MailException e){
                    log.error("Failed to send confirmation email.", e);
                    return Result.error("Failed to send confirmation email.");
                }

                // 在数据库中标记此管理员为“未确认”
//                userMapper.registerAdmin(uuid, username, encodedPassword, adminEmail, false);
                return Result.success(null, "Please confirm your registration via email.");
            }
        } catch (Exception e) {
            log.error("User registration failed.", e);
            return Result.error("User registration failed.");
        }
    }

}
