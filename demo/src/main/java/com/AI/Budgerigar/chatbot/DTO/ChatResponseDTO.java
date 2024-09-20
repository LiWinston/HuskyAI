package com.AI.Budgerigar.chatbot.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未映射的字段
@ToString
public class ChatResponseDTO {

    private String id;

    private String object;

    private long created;

    private String model;

    private Usage usage;

    private List<Choice> choices;

    private String system_fingerprint;

    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    public static class Choice {

        private int index;

        private OAMessageDTO message; // 用于同步响应

        private OAMessageDTO delta; // 用于流式响应的命名映射

        private String finish_reason; // 完整响应中的字段

    }

    private String logprobs; // 完整响应中的字段

    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    public static class Usage {

        private int prompt_tokens;

        private int completion_tokens;

        private int total_tokens;

    }

}
