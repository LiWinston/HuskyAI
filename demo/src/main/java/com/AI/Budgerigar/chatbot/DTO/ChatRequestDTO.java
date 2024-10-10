package com.AI.Budgerigar.chatbot.DTO;

import com.AI.Budgerigar.chatbot.Entity.Message;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@lombok.Getter
@lombok.Setter
@ToString
public class ChatRequestDTO {

    private String model;

    private List<OAMessageDTO> messages;

    // private int maxTokens = 300;

    private boolean stream = false;

    private double temperature = 0.7; // Default value, adjust as needed.

    // The original constructor is retained to handle a single prompt.
    public ChatRequestDTO(String model, String prompt, Instant timestamp) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new OAMessageDTO("user", prompt));
    }

    // Private constructor.
    private ChatRequestDTO(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages.stream()
            .map(message -> new OAMessageDTO(message.getRole(), message.getContent()))
            .collect(Collectors.toList());
    }

    // Factory method for creating ChatRequestDTO from List<Message>.
    public static ChatRequestDTO fromMessages(String model, List<Message> messages) {
        return new ChatRequestDTO(model, messages);
    }

    // A factory method for creating ChatRequestDTO from List<String[]>.
    public static ChatRequestDTO fromStringTuples(String model, List<String[]> stringPairList) {
        List<Message> messages = new ArrayList<>();
        for (String[] stringPair : stringPairList) {
            messages.add(new Message(stringPair[0], stringPair[1], stringPair[2]));
        }
        return new ChatRequestDTO(model, messages);
    }

}
