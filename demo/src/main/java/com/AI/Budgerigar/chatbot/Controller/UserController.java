package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.DTO.loginResponseDTO;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import com.AI.Budgerigar.chatbot.security.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.AI.Budgerigar.chatbot.Controller.Utils.UsernameSuggestionUtil.generateUsernameSuggestions;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserMapper userMapper; // Maps database queries related to user operations
                                   // 映射与用户操作相关的数据库查询

    @Autowired
    private userService userService; // Provides services for user operations 提供用户操作的服务

    @Autowired
    private PasswordEncoder passwordEncoder; // Encodes passwords for secure storage
                                             // 对密码进行编码以确保安全存储

    @Autowired
    private JwtTokenUtil jwtTokenUtil; // Utility for generating JWT tokens 用于生成JWT令牌的工具类

    private static final int MAX_SUGGESTIONS = 3; // Maximum number of username
                                                  // suggestions 用户名建议的最大数量

    private static final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    // Levenshtein Distance algorithm for suggesting similar usernames
    // 用于建议类似用户名的Levenshtein距离算法

    /**
     * Registers a new user. Calls the user service to handle registration logic.
     * 用户注册，调用userService来处理注册逻辑。
     * @param request the HttpServletRequest object from the client
     * 客户端的HttpServletRequest对象
     * @param userRegisterDTO the DTO containing user registration details 包含用户注册详细信息的DTO
     * @return a Result object indicating the success or failure of the registration
     * 返回注册成功或失败的结果对象
     */
    @PostMapping("/register")
    public Result<?> register(HttpServletRequest request, @RequestBody UserRegisterDTO userRegisterDTO) {
        return userService.register(request, userRegisterDTO);
    }

    /**
     * Confirms admin registration using a token. 使用令牌确认管理员注册。
     * @param token the token used for confirmation 用于确认的令牌
     * @return a Result object indicating the success or failure of the confirmation
     * 返回确认成功或失败的结果对象
     */
    @GetMapping("/register/confirm/{token}")
    public Result<?> confirmAdmin(@PathVariable("token") String token) {
        return userService.confirmAdmin(token);
    }

    /**
     * Logs in a user by checking username and password. If successful, generates a JWT
     * token. 通过检查用户名和密码登录用户，成功则生成JWT令牌。
     * @param userDetails a map containing username and password 包含用户名和密码的映射
     * @return a Result object with the UUID and JWT token if login is successful
     * 成功登录时返回带有UUID和JWT令牌的结果对象
     */
    @PostMapping("/login")
    public loginResponseDTO login(@RequestBody Map<String, String> userDetails) {
        String username = userDetails.get("username");
        String password = userDetails.get("password");

        try {
            UserPw user = userMapper.getUserByUsername(username);
            if (user == null) {
                return loginResponseDTO.builder().code(0).msg("User not found").build(); // 用户不存在
            }
            else if (!passwordEncoder.matches(password, user.getPassword())) {
                return loginResponseDTO.builder().code(0).msg("Incorrect password").build(); // 密码错误
            }
            String token = jwtTokenUtil.generateToken(user.getUuid());
            StringBuilder msg = new StringBuilder("Login successful");
            var lginRspDto = loginResponseDTO.builder()
                .code(1)
                .token(token)
                .username(user.getUsername())
                .uuid(user.getUuid())
                .role(user.getRole());
            if (user.getRole().equals("admin")) {
                var res = userService.checkUserIsAdminByUuid(user.getUuid());
                lginRspDto.confirmedAdmin(res.getData());
                msg.append(" Admin status: ").append(res.getMsg());
                lginRspDto.msg(msg.toString());
                // lginRspDto.msg(lginRspDto.getMsg() + " Admin: " + res.getData());
            }
            return lginRspDto.build();
        }
        catch (Exception e) {
            log.error("Login failed.", e); // 登录失败
            return loginResponseDTO.builder().code(0).msg("Login failed" + e.getMessage()).build();
        }
    }

    /**
     * Checks if a username is available. If not, generates username suggestions.
     * 检查用户名是否可用，如果不可用则生成用户名建议。
     * @param username the username to check 要检查的用户名
     * @return a Result object indicating whether the username is available or not
     * 返回用户名是否可用的结果对象
     */
    @GetMapping("/register/checkUsername")
    public Result<?> checkUsername(@RequestParam String username) {
        try {
            if (userMapper.getUserByUsername(username) != null) {
                List<String> suggestions = generateUsernameSuggestions(userMapper, username);
                return Result.error(suggestions, "Username already exists."); // 用户名已存在，返回建议
            }
            return Result.success(null, "Username is available."); // 用户名可用
        }
        catch (Exception e) {
            log.error("Username check failed.", e); // 用户名检查失败
            return Result.error("Username check failed."); // 返回用户名检查失败信息
        }
    }

}