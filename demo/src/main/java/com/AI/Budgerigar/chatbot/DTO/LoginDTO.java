package com.AI.Budgerigar.chatbot.DTO;

import com.AI.Budgerigar.chatbot.Nosql.UserIpInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginDTO {

    private String username;

    private String password;

    private UserIpInfoDTO userIpInfo;

    private class UserIpInfoDTO {
        private String continent;
        private String country;
        private String owner;
        private String isp;
        private String zipcode;
        private String timezone;
        private String accuracy;
        private String source;
        private String areacode;
        private String adcode;
        private String asnumber;
        private String lat;
        private String lng;
        private String radius;
        private String prov;
        private String city;
        private String district;
        private String coordsys;
    }
}
