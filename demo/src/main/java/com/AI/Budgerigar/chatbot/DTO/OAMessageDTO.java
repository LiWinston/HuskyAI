package com.AI.Budgerigar.chatbot.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@lombok.Setter
@lombok.Getter
@Builder
public class OAMessageDTO {

    private String role;

    private String content;

}
