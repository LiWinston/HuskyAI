package com.AI.Budgerigar.chatbot.Nosql;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

@Document(collection = "user_ip_info")
@Data
@ToString
public class UserIpInfo {

    @Id
    private String id; // MongoDB自带的唯一ID
    private String userUuid; // 用户的唯一ID


}

