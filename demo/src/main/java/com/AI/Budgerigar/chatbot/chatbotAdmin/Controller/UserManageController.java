package com.AI.Budgerigar.chatbot.chatbotAdmin.Controller;

import com.AI.Budgerigar.chatbot.Entity.UserPw;
import com.AI.Budgerigar.chatbot.Entity.AdminInfo;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
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

    /**
     * 验证管理员基本权限（适用于查看操作）
     */
    private UserPw validateBasicAdminAccess(String operatorUUID) {
        if (operatorUUID == null) {
            log.error("Authentication required: operatorUUID is null");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        log.info("Validating admin access for UUID: {}", operatorUUID);
        UserPw operator = userMapper.getUserByUuid(operatorUUID);
        
        if (operator == null) {
            log.error("User not found for UUID: {}", operatorUUID);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        
        log.info("User found: {} with role: {}", operator.getUsername(), operator.getRole());
        if (operator.getRole() == null || !operator.getRole().equalsIgnoreCase("ADMIN")) {
            log.error("Insufficient permissions for user: {} with role: {}", operator.getUsername(), operator.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
        
        // 检查管理员信息是否存在
        AdminInfo adminInfo = userMapper.getAdminInfoByUuid(operatorUUID);
        if (adminInfo == null) {
            log.error("Admin info not found for admin user: {}", operator.getUsername());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin info not found");
        }
        
        log.info("Admin validation successful for user: {} with level: {}", operator.getUsername(), adminInfo.getAdminLevel());
        return operator;
    }

    /**
     * 验证管理员修改操作权限
     */
    private void validateAdminModifyOperation(String operatorUUID, String targetUserUUID) {
        UserPw operator = validateBasicAdminAccess(operatorUUID);
        AdminInfo operatorAdminInfo = userMapper.getAdminInfoByUuid(operatorUUID);

        if (targetUserUUID != null) {
            UserPw targetUser = userMapper.getUserByUuid(targetUserUUID);
            if (targetUser != null && targetUser.getRole() != null && targetUser.getRole().equalsIgnoreCase("ADMIN")) {
                AdminInfo targetAdminInfo = userMapper.getAdminInfoByUuid(targetUserUUID);
                if (targetAdminInfo != null) {
                    // 检查管理员级别
                    int operatorLevel = operatorAdminInfo != null ? operatorAdminInfo.getAdminLevel() : 0;
                    int targetLevel = targetAdminInfo.getAdminLevel();
                    
                    if (operatorLevel <= targetLevel) {
                        throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, 
                            "Insufficient admin level: cannot modify admin with equal or higher level"
                        );
                    }
                }
            }
        }
    }

    @GetMapping
    @Operation(summary = "Obtain all users", description = "Obtain all user information.")
    public Result<List<UserPw>> getAllUsers(@RequestHeader(value = "X-User-UUID", required = false) String operatorUUID) {
        try {
            log.info("Received getAllUsers request with UUID: {}", operatorUUID);
            // 只验证基本管理员权限
            validateBasicAdminAccess(operatorUUID);
            List<UserPw> users = userMapper.selectAll();
            return Result.success(users);
        } catch (ResponseStatusException e) {
            log.error("Error in getAllUsers: {}", e.getMessage());
            return Result.error(e.getReason());
        } catch (Exception e) {
            log.error("Error fetching all users", e);
            return Result.error("Error fetching all users: " + e.getMessage());
        }
    }

    @GetMapping("/modelAccess")
    @Operation(summary = "get model access permissions for all users",
            description = "Get model access permissions for all users")
    public Result<List<UserModelAccessConfig>> getAllUsersModelAccess(
            @RequestHeader(value = "X-User-UUID", required = false) String operatorUUID) {
        try {
            log.info("Received getAllUsersModelAccess request with UUID: {}", operatorUUID);
            // 只验证基本管理员权限
            validateBasicAdminAccess(operatorUUID);
            List<UserModelAccessConfig> users = userModelAccessConfigRepository.findAll();
            return Result.success(users);
        } catch (ResponseStatusException e) {
            log.error("Error in getAllUsersModelAccess: {}", e.getMessage());
            return Result.error(e.getReason());
        } catch (Exception e) {
            log.error("Error fetching all users model access", e);
            return Result.error("Error fetching all users model access: " + e.getMessage());
        }
    }

    @PutMapping("/modelAccess/{userId}")
    @Operation(summary = "Update model access for a user",
            description = "Update model access permissions for a specific user")
    public Result<Void> updateUserModelAccess(
            @PathVariable String userId,
            @RequestBody List<ModelAccessDTO> allowedModelDTOs,
            @RequestHeader(value = "X-User-UUID", required = false) String operatorUUID) {
        try {
            // 验证修改权限
            validateAdminModifyOperation(operatorUUID, userId);
            
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

            userModelAccessService.updateUserAccessConfig(userId, newModelAccess);
            return Result.success();
        } catch (ResponseStatusException e) {
            return Result.error(e.getReason());
        } catch (Exception e) {
            log.error("Error updating user model access", e);
            return Result.error("Error updating user model access: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}")
    public Result<?> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, String> updateData,
            @RequestHeader(value = "X-User-UUID", required = false) String operatorUUID) {
        try {
            // 验证修改权限
            validateAdminModifyOperation(operatorUUID, userId);
            
            UserPw user = userMapper.getUserByUuid(userId);
            if (user == null) {
                return Result.error("User not found");
            }

            String newRole = updateData.get("role");
            if (newRole == null) {
                return Result.error("Role must be specified");
            }

            boolean isRoleChange = !newRole.equals(user.getRole());
            boolean isBecomingAdmin = isRoleChange && "ADMIN".equals(newRole);
            boolean isBecomingUser = isRoleChange && "USER".equals(newRole);

            if (isBecomingAdmin || "ADMIN".equals(user.getRole())) {
                String email = updateData.get("email");
                if (email == null || email.isBlank()) {
                    return Result.error("Email must be specified for admin");
                }

                int adminLevel;
                try {
                    adminLevel = Integer.parseInt(updateData.get("adminLevel"));
                    if (adminLevel < 0 || adminLevel > 3) {
                        return Result.error("Admin level must be between 0 and 3");
                    }
                    
                    // 检查操作者不能设置比自己更高级别的管理员
                    AdminInfo operatorAdminInfo = userMapper.getAdminInfoByUuid(operatorUUID);
                    int operatorLevel = operatorAdminInfo != null ? operatorAdminInfo.getAdminLevel() : 0;
                    if (adminLevel >= operatorLevel) {
                        return Result.error("Cannot set admin level equal to or higher than your own level");
                    }
                } catch (NumberFormatException e) {
                    return Result.error("Invalid admin level format");
                }

                if (isBecomingAdmin) {
                    userMapper.promoteToAdminByUuid(userId);
                    userModelAccessService.grantAllAvailiableModels(userId);
                }

                // 更新管理员信息
                userMapper.registerAdmin(userId, user.getUsername(), user.getPassword(), email, true);
                
                return Result.success(true, isBecomingAdmin ? "User promoted to admin successfully" : "Admin info updated successfully");
            } else if (isBecomingUser) {
                userMapper.downgradeAdminByUuid(userId);
                return Result.success(true, "Admin demoted to user successfully");
            } else {
                return Result.success(true, "No changes required");
            }
        } catch (ResponseStatusException e) {
            return Result.error(e.getReason());
        } catch (Exception e) {
            log.error("Error updating user role: ", e);
            return Result.error(e.getMessage());
        }
    }
}
