package com.AI.Budgerigar.chatbot.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Conversation implements Serializable {

    private String conversationId;

    private String firstMessage;

    private LocalDateTime createdAt; // Conversation start time

    private LocalDateTime lastMessageAt; // Last message time

}
