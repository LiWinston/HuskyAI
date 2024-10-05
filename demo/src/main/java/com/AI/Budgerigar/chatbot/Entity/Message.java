package com.AI.Budgerigar.chatbot.Entity;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@lombok.Setter
@lombok.Getter
public class Message {

    private String role;

    private String timestamp;

    private String content;

    public String toString() {
        return role + "|" + timestamp + "|" + content;
    }

}