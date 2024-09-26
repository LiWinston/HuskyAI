package com.AI.Budgerigar.chatbot.Nosql;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_ip_info")
@Data
@ToString
@Builder
public class UserIpInfo {

    @Id
    private String id; // MongoDB自带的唯一ID

    private String userUuid; // 用户的唯一ID

    private String areaCode;

    private String city;

    private String cityCode;

    private String continent;

    private String country;

    private String countryEnglish;

    private String district;

    private String elevation;

    private String ip;

    private String isp;

    private String lat;

    private String lng;

    private String prov;

    private String timeZone;

    private String weatherStation;

    private String zipCode;

}
