package com.AI.Budgerigar.chatbot.DTO;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class loginResponseDTO {

    private int code;

    private String msg;

    private String token;

    private String username;

    private String uuid;

    private String role;

    private Boolean confirmedAdmin = false;

}
