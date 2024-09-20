package com.AI.Budgerigar.chatbot.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

@AllArgsConstructor
@lombok.Setter
@lombok.Getter
@Builder
@ToString
public class OAMessageDTO {

    private String role;

    private String content;

}
