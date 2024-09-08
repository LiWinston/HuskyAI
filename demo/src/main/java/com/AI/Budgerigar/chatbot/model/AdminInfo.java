package com.AI.Budgerigar.chatbot.model;

import lombok.Data;

@Data
public class AdminInfo {

    private String uuid;

    private int adminLevel;

    private String email;

    private boolean verified;

}
