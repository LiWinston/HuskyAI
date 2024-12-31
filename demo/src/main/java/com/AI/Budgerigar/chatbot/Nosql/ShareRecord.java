package com.AI.Budgerigar.chatbot.Nosql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@Document(collection = "share_records")
public class ShareRecord {

    private String shareCode;

    private String uuid;

    private String conversationId;

    private List<Integer> messageIndexes;

    @Indexed(expireAfterSeconds = 0)  // TTL索引，到期后自动删除
    private Date expireAt;  // 过期时间

    private Date createdAt;  // 创建时间
}