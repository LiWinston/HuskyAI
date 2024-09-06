package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import com.AI.Budgerigar.chatbot.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // 用户注册
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> userDetails) {
        String username = userDetails.get("username");
        String password = userDetails.get("password");

        try {
            if (userMapper.getUserByUsername(username) != null) {
                return Result.error("Username already exists.");
            }
            String uuid = java.util.UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);
            userMapper.registerUser(uuid, username, encodedPassword);
            return Result.success(null, "User registered successfully.");
        } catch (Exception e) {
            return Result.error("User registration failed.");
        }
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> userDetails) {
        String username = userDetails.get("username");
        String password = userDetails.get("password");

        try {
            UserPw user = userMapper.getUserByUsername(username);
            if (user == null) {
                return Result.error("User does not exist.");
            }else if(!passwordEncoder.matches(password, user.getPassword())){
                return Result.error("Incorrect password.");
            }
            String uuid = user.getUuid();
            String token = jwtTokenUtil.generateToken(user.getUuid());
            return Result.success(uuid, token);
        } catch (Exception e) {
            return Result.error("Login failed.");
        }
    }
}
