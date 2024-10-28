package com.AI.Budgerigar.chatbot.chatbotAdmin.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ManageModelDTO {

    private String name;

    private List<String> models;

    private String operation; // "enable" or "disable"

}