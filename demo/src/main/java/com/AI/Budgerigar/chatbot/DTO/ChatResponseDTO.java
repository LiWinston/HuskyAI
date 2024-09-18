package com.AI.Budgerigar.chatbot.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未映射的字段
public class ChatResponseDTO {

    private String id;

    private String object;

    private long created;

    private String model;

    private Usage usage;

    private List<Choice> choices;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Choice {

        private int index;

        private OAMessageDTO message;

        private String finish_reason; // 完整响应中的字段

    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Usage {

        private int prompt_tokens;

        private int completion_tokens;

        private int total_tokens;

    }

}
