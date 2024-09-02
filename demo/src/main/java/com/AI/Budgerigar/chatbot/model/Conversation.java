package com.AI.Budgerigar.chatbot.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
public class Conversation implements Serializable {
    private String conversationId;
    private String firstMessage;
}
