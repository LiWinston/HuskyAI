package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.DTO.LoginDTO;
import com.AI.Budgerigar.chatbot.DTO.UserIpInfoDTO;
import com.AI.Budgerigar.chatbot.DTO.UserRegisterDTO;
import com.AI.Budgerigar.chatbot.DTO.loginResponseDTO;
import com.AI.Budgerigar.chatbot.Services.LoginIpService;
import com.AI.Budgerigar.chatbot.Services.UserModelAccessService;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import com.AI.Budgerigar.chatbot.security.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.AI.Budgerigar.chatbot.Controller.Utils.UsernameSuggestionUtil.generateUsernameSuggestions;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserMapper userMapper; // Maps database queries related to user operations

    @Autowired
    private ExecutorService excecutorService;

    @Autowired
    private UserModelAccessService userModelAccessService;

    @Autowired
    private userService userService; // Provides services for user operations

    @Autowired
    private PasswordEncoder passwordEncoder; // Encodes passwords for secure storage

    @Autowired
    private JwtTokenUtil jwtTokenUtil; // Utility for generating JWT tokens

    private static final int MAX_SUGGESTIONS = 3; // Maximum number of username

    private static final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    @Autowired
    private LoginIpService loginIpService;

    // Levenshtein Distance algorithm for suggesting similar usernames

    /**
     * Registers a new user. Calls the user service to handle registration logic.
     * @param request the HttpServletRequest object from the client
     * @param userRegisterDTO the DTO containing user registration details
     * @return a Result object indicating the success or failure of the registration
     */
    @PostMapping("/register")
    public Result<?> register(HttpServletRequest request, @RequestBody UserRegisterDTO userRegisterDTO) {
        return userService.register(request, userRegisterDTO);
    }

    /**
     * Confirms admin registration using a token.
     * @param token the token used for confirmation
     * @return a Result object indicating the success or failure of the confirmation
     */
    @GetMapping("/register/confirm/{token}")
    public Result<?> confirmAdmin(@PathVariable("token") String token) {
        return userService.confirmAdmin(token);
    }

    @PostMapping("/login/ip")
    public void loginIp(@RequestBody UserIpInfoDTO userIpInfo) {
        String username = userIpInfo.getLoginDTO().getUsername();
        String pwd = userIpInfo.getLoginDTO().getPassword();
        UserPw userPw = userMapper.getUserByUsername(username);
        if (!passwordEncoder.matches(pwd, userPw.getPassword())) {
            return;
        }
        excecutorService.submit(() -> {
            LoginIpService.LoginIpStatus loginIpStatus = loginIpService.handleLoginIp(userPw,
                    userIpInfo.getIpInfoDTO());
            log.info("Login IP status : {} : {}", username, loginIpStatus);
        });
    }

    /**
     * Logs in a user by checking username and password. If successful, generates a JWT token.
     * @param userDetails a map containing username and password
     * @return a Result object with the UUID and JWT token if login is successful
     */
    @PostMapping("/login")
    public loginResponseDTO login(@RequestBody LoginDTO userDetails) {
        String username = userDetails.getUsername();
        String password = userDetails.getPassword();

        log.info("Login request: {}", userDetails);
        try {
            UserPw user = userMapper.getUserByUsername(username);
            if (user == null) {
                return loginResponseDTO.builder().code(0).msg("User not found").build(); // user not found.
            }
            else if (!passwordEncoder.matches(password, user.getPassword())) {
                return loginResponseDTO.builder().code(0).msg("Incorrect password").build(); // password incorrect.
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

            excecutorService.execute(() -> {
                userModelAccessService.grantAllAvailiableModels(user.getUuid());
            });
            return lginRspDto.build();
        }
        catch (Exception e) {
            log.error("Login failed.", e); // Failed to login.
            return loginResponseDTO.builder().code(0).msg("Login failed" + e.getMessage()).build();
        }
    }

    /**
     * Checks if a username is available. If not, generates username suggestions.
     * @param username the username to check
     * @return a Result object indicating whether the username is available or not
     */
    @GetMapping("/register/checkUsername")
    public Result<?> checkUsername(@RequestParam String username) {
        try {
            if (userMapper.getUserByUsername(username) != null) {
                List<String> suggestions = generateUsernameSuggestions(userMapper, username);
                return Result.error(suggestions, "Username already exists."); // username already exists
            }
            return Result.success(null, "Username is available."); // return success message
        }
        catch (Exception e) {
            log.error("Username check failed.", e); // username check failed
            return Result.error("Username check failed."); // return error
        }
    }

}