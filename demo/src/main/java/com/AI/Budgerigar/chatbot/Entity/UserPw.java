package com.AI.Budgerigar.chatbot.Entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPw implements Serializable {

    private String uuid;

    private String username;

    private String password;

    private String role;

    private String email;

    private boolean enabled;

    private String ssoId;

}
