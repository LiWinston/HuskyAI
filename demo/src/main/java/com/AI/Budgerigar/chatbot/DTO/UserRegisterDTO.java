package com.AI.Budgerigar.chatbot.DTO;

import lombok.Data;

@Data
public class UserRegisterDTO {

    private String username;

    private String password;

    private Boolean isAdmin;

    private String adminEmail;

}
