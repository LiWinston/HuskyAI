package com.AI.Budgerigar.chatbot.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginDTO {

    private String username;

    private String password;

    //
    // @JsonProperty("UserIpInfo") // 这里指定 JSON 中的 "UserIpInfo" 对应 Java 中的 "userIpInfo"
    // private UserIpInfoDTO userIpInfo;

}
