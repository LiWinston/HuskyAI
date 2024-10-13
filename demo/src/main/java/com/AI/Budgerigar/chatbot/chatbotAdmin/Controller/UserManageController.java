package com.AI.Budgerigar.chatbot.chatbotAdmin.Controller;

import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfig;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfigRepository;
import com.AI.Budgerigar.chatbot.Services.UserModelAccessService;
import com.AI.Budgerigar.chatbot.chatbotAdmin.DTO.ModelAccessDTO;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Admin interface user management",
        description = "Modify user attributes and add or remove user model access permissions.")
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
    @Operation(summary = "Obtain all users", description = "Obtain all user information.")
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
    @Operation(summary = "get model access permissions for all users",
            description = "Get model access permissions for all users")
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
    @Operation(summary = "Update model access for a user",
            description = "Update model access permissions for a specific user")
    public Result<Void> updateUserModelAccess(@PathVariable String userId,
            @RequestBody List<ModelAccessDTO> allowedModelDTOs) {
        try {
            // Convert DTO to persistent entity
            List<UserModelAccessConfig.ModelAccess> newModelAccess = allowedModelDTOs.stream().map(dto -> {
                UserModelAccessConfig.ModelAccess modelAccess = new UserModelAccessConfig.ModelAccess();
                BeanUtils.copyProperties(dto, modelAccess);
                if (dto.getAccessRestriction() != null) {
                    UserModelAccessConfig.AccessRestriction accessRestriction = new UserModelAccessConfig.AccessRestriction();
                    BeanUtils.copyProperties(dto.getAccessRestriction(), accessRestriction);
                    modelAccess.setAccessRestriction(accessRestriction);
                }
                return modelAccess;
            }).collect(Collectors.toList());

            // Call the service layer to update the user model access permission
            // configuration
            userModelAccessService.updateUserAccessConfig(userId, newModelAccess);

            return Result.success();
        }
        catch (Exception e) {
            log.error("Error updating user model access", e);
            return Result.error("Error updating user model access: " + e.getMessage());
        }
    }

}
