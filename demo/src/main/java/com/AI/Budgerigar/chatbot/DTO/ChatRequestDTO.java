package com.AI.Budgerigar.chatbot.DTO;

import com.AI.Budgerigar.chatbot.AIUtil.Message;

import java.util.ArrayList;
import java.util.List;

@lombok.Getter
@lombok.Setter
public class ChatRequestDTO {

    private String model;
    private List<Message> messages;
    private int n = 1; // 默认值，根据需要调整
    private double temperature = 0.7; // 默认值，根据需要调整

    // 原有的构造函数，保留以处理单一的prompt
    public ChatRequestDTO(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }

    // 私有的构造函数
    private ChatRequestDTO(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    // 工厂方法，用于从 List<Message> 创建 ChatRequestDTO
    public static ChatRequestDTO fromMessages(String model, List<Message> messages) {
        return new ChatRequestDTO(model, messages);
    }

    // 工厂方法，用于从 List<String[]> 创建 ChatRequestDTO
    public static ChatRequestDTO fromStringPairs(String model, List<String[]> stringPairList) {
        List<Message> messages = new ArrayList<>();
        for (String[] stringPair : stringPairList) {
            messages.add(new Message(stringPair[0], stringPair[1]));
        }
        return new ChatRequestDTO(model, messages);
    }
}
