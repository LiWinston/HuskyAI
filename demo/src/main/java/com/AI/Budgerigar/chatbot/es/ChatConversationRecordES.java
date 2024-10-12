package com.AI.Budgerigar.chatbot.es;

import com.AI.Budgerigar.chatbot.Entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Document(indexName = "chat_conversations")
public class ChatConversationRecordES {

    @Id
    private String conversationId;

    private List<String[]> messages;

    public ChatConversationRecordES() {
        this.messages = new ArrayList<>();
    }

    // Add a method that accepts a list of Message types.
    public void addMessages(List<Message> newMessages) {
        List<String[]> messageArrays = newMessages.stream()
            .map(message -> new String[] { message.getRole(), message.getTimestamp(), message.getContent() })
            .toList();
        this.messages.addAll(messageArrays);
    }

    // Add a method that accepts a list of type String[].
    public void addStringMessages(List<String[]> newMessages) {
        this.messages.addAll(newMessages);
    }

}