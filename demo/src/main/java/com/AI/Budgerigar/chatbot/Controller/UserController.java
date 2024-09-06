package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.result.Result;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // 用户注册
    @PostMapping("/register")
    public Result<?> register(@RequestParam String username, @RequestParam String password) {
        // 检查用户名是否已存在
        if (userMapper.getUserByUsername(username) != null) {
            return Result.error("Username already exists.");
        }
        // 生成UUID
        String uuid = java.util.UUID.randomUUID().toString();
        // 加密密码
        String encodedPassword = passwordEncoder.encode(password);
        // 注册用户
        userMapper.registerUser(uuid, username, encodedPassword);
        return Result.success("User registered successfully.");
    }

    // 用户登录
    @PostMapping("/login")
    public Result<?> login(@RequestParam String username, @RequestParam String password) {
        UserPw user = userMapper.getUserByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("Invalid username or password.");
        }
        // 生成 JWT
        String token = jwtTokenUtil.generateToken(user.getUuid());
        return Result.success(token);
    }
}
