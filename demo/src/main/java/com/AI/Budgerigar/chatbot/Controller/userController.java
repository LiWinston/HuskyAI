package com.AI.Budgerigar.chatbot.Controller;


import com.AI.Budgerigar.chatbot.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class userController {

    //traditional login with username and password
    //传统登录方式，用户名和密码
    @PostMapping("/login")
    public Result<?> login(String username, String password) {



        return Result.error("Not implemented");
    }

    //Traditional registration with username and password
    //传统注册方式，用户名和密码
    @PostMapping("/register")
    public Result<?> register(String username, String password) {
        return Result.error("Not implemented");
    }

}
