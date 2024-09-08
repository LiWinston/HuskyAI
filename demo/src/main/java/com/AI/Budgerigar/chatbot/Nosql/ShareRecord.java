package com.AI.Budgerigar.chatbot.Nosql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

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

}