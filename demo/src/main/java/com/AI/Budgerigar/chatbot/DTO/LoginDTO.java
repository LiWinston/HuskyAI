package com.AI.Budgerigar.chatbot.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginDTO {

    private String username;

    private String password;

    //
    // @JsonProperty("UserIpInfo")
    // private UserIpInfoDTO userIpInfo;

}
