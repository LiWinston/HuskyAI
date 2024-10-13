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
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unmapped fields.
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

        private OAMessageDTO message; // Used for synchronous response.

        private OAMessageDTO delta; // Naming map for streaming response.

        private String finish_reason; // Fields in the complete response.

    }

    private String logprobs; // Fields in the complete response.

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
