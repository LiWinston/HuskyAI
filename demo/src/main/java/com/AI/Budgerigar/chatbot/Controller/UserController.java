package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import com.AI.Budgerigar.chatbot.security.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
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
            log.error("User registration failed.", e);
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
            log.error("Login failed.", e);
            return Result.error("Login failed.");
        }
    }

    @GetMapping("/register/checkUsername")
    public Result<?> checkUsername(@RequestParam String username) {
        try {
            if (userMapper.getUserByUsername(username) != null) {
                // 假设 userMapper 有方法返回最接近的替代用户名
                List<String> suggestions = generateUsernameSuggestions(username);
                return Result.error(suggestions, "Username already exists.");
            }
            return Result.success(null, "Username is available.");
        } catch (Exception e) {
            log.error("Username check failed.", e);
            return Result.error("Username check failed.");
        }
    }

    private List<String> generateUsernameSuggestions(String username) {
        // 实现生成最接近用户名的算法
        List<String> suggestions = new ArrayList<>();
        suggestions.add(username + "123");
        suggestions.add(username + "_official");
        suggestions.add(username + "_01");
        return suggestions;
    }
}
