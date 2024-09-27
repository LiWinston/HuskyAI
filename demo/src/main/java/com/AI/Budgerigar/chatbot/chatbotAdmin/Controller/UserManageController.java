package com.AI.Budgerigar.chatbot.chatbotAdmin.Controller;

import com.AI.Budgerigar.chatbot.Services.UserModelAccessService;
import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import com.AI.Budgerigar.chatbot.model.UserPw;
import com.AI.Budgerigar.chatbot.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
