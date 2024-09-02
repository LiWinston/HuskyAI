package com.AI.Budgerigar.chatbot.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPw implements Serializable {

    private String uuid;

    private String username;

    private String password;

    // getters and setters

}
