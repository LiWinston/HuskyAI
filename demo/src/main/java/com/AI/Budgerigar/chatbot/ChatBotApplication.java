package com.AI.Budgerigar.chatbot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.AI.Budgerigar.chatbot")
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan("com.AI.Budgerigar.chatbot.mapper")

public class ChatBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatBotApplication.class, args);
    }

}
