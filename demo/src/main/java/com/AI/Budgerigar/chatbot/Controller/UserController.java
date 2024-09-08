package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import com.AI.Budgerigar.chatbot.security.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private userService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private static final int MAX_SUGGESTIONS = 3;

    private static final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    // 用户注册
    @PostMapping("/register")
    public Result<?> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        return userService.register(userRegisterDTO);
    }

    // 用户登录
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> userDetails) {
        String username = userDetails.get("username");
        String password = userDetails.get("password");

        try {
            UserPw user = userMapper.getUserByUsername(username);
            if (user == null) {
                return Result.error("User does not exist.");
            }
            else if (!passwordEncoder.matches(password, user.getPassword())) {
                return Result.error("Incorrect password.");
            }
            String token = jwtTokenUtil.generateToken(user.getUuid());
            return Result.success(user.getUuid(), token);
        }
        catch (Exception e) {
            log.error("Login failed.", e);
            return Result.error("Login failed.");
        }
    }

    // 检查用户名可用性
    @GetMapping("/register/checkUsername")
    public Result<?> checkUsername(@RequestParam String username) {
        try {
            if (userMapper.getUserByUsername(username) != null) {
                List<String> suggestions = generateUsernameSuggestions(username);
                return Result.error(suggestions, "Username already exists.");
            }
            return Result.success(null, "Username is available.");
        }
        catch (Exception e) {
            log.error("Username check failed.", e);
            return Result.error("Username check failed.");
        }
    }

    // 生成基于 Levenshtein 距离的最小改动用户名建议
    private List<String> generateUsernameSuggestions(String username) {
        List<String> candidates = new ArrayList<>();

        // Step 1: Generate various candidates
        candidates.add(username.toLowerCase());
        candidates.add(username.toUpperCase());
        candidates.add(capitalizeFirstLetter(username));

        if (!username.contains("_")) {
            candidates.add(insertUnderscore(username, 1));
            candidates.add(insertUnderscore(username, username.length() / 2));
        }

        candidates.add(username + "1");
        candidates.add(username + "_01");
        candidates.add(username + "123");

        // Step 2: Ensure suggestions are unique in database
        List<String> validCandidates = new ArrayList<>();
        for (String candidate : candidates) {
            if (userMapper.getUserByUsername(candidate) == null) {
                validCandidates.add(candidate);
            }
        }

        // Step 3: Sort based on Levenshtein Distance
        validCandidates.sort(Comparator.comparingInt(c -> levenshteinDistance.apply(username, c)));

        // Step 4: Return top N suggestions
        return validCandidates.subList(0, Math.min(MAX_SUGGESTIONS, validCandidates.size()));
    }

    // Capitalize first letter
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    // Insert underscore at a specific position
    private String insertUnderscore(String input, int position) {
        if (position < 0 || position > input.length()) {
            return input;
        }
        return new StringBuilder(input).insert(position, "_").toString();
    }

}
