package com.AI.Budgerigar.chatbot.chatbotAdmin.Controller;

import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfig;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfigRepository;
import com.AI.Budgerigar.chatbot.Services.UserModelAccessService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理员界面用户管理", description = "修改用户属性、增删用户的模型访问权限")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/user")
@Slf4j
public class UserManageController {

    @Autowired
    private UserModelAccessService userModelAccessService;

    @Autowired
    UserModelAccessConfigRepository userModelAccessConfigRepository;

    @Autowired
    private UserMapper userMapper;

    @GetMapping
    @Operation(summary = "获取所有用户", description = "获取所有用户的信息")
    public Result<List<UserPw>> getAllUsers() {
        try {
            List<UserPw> users = userMapper.selectAll();
            return Result.success(users);
        }
        catch (Exception e) {
            log.error("Error fetching all users", e);
            return Result.error("Error fetching all users" + e);
        }
    }

    @GetMapping("/modelAccess")
    @Operation(summary = "get model access permissions for all users", description = "Get model access permissions for all users")
    public Result<List<UserModelAccessConfig>> getAllUsersModelAccess() {
        try {
            List<UserModelAccessConfig> users = userModelAccessConfigRepository.findAll();
            return Result.success(users);
        }
        catch (Exception e) {
            log.error("Error fetching all users model access", e);
            return Result.error("Error fetching all users model access" + e);
        }
    }

    @PutMapping("/modelAccess/{userId}")
    @Operation(summary = "Update model access for a user", description = "Update model access permissions for a specific user")
    public Result<Void> updateUserModelAccess(@PathVariable String userId, @RequestBody List<UserModelAccessConfig.ModelAccess> allowedModels) {
        try {
            userModelAccessService.updateUserAccessConfig(userId, allowedModels);
            return Result.success();
        } catch (Exception e) {
            log.error("Error updating user model access", e);
            return Result.error("Error updating user model access: " + e.getMessage());
        }
    }

}
