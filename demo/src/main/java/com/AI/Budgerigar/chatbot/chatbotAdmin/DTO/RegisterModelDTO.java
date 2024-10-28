package com.AI.Budgerigar.chatbot.chatbotAdmin.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RegisterModelDTO {

    private String url;

    private String name;

    private String apiKey; // Optional field, can be null.

    private List<String> allowedModels; // Optional field, can be empty list.

}