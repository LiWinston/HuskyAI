package com.AI.Budgerigar.chatbot.DTO;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@lombok.Setter
@lombok.Getter
public class ChatRequest {

    private String model;
    private List<Message> messages;
    private int n;
    private double temperature;

    public ChatRequest(String model, String prompt) {
        this.model = model;

        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }


}