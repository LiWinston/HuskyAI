package com.AI.Budgerigar.chatbot.DTO;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import lombok.AllArgsConstructor;

import java.util.List;

//@Deprecated
@AllArgsConstructor
@lombok.Setter
@lombok.Getter
public class ChatResponseDTO {

    private List<Choice> choices;

    @AllArgsConstructor
    @lombok.Setter
    @lombok.Getter
    public static class Choice {

        private int index;
        private Message message;
    }
}