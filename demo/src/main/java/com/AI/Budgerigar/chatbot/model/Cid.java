package com.AI.Budgerigar.chatbot.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Cid implements Serializable {
    private String conversationId;
    private String firstMessage;
}
