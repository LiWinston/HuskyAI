package com.AI.Budgerigar.chatbot.Nosql;

import com.AI.Budgerigar.chatbot.Entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Document(collection = "chat_conversations")
public class ChatConversationRecord {

    @Id
    private String conversationId;

    private List<String[]> messages;

    public ChatConversationRecord() {
        this.messages = new ArrayList<>();
    }

    public void setMessages(List<String[]> messages) {
        this.messages = messages;
    }

    public List<String[]> getMessages() {
        return messages;
    }

    // 添加一个接受 Message 类型列表的方法
    public void addMessages(List<Message> newMessages) {
        List<String[]> messageArrays = newMessages.stream()
            .map(message -> new String[] { message.getRole(), message.getTimestamp(), message.getContent() })
            .collect(Collectors.toList());
        this.messages.addAll(messageArrays);
    }

    // 添加一个接受 String[] 类型列表的方法
    public void addStringMessages(List<String[]> newMessages) {
        this.messages.addAll(newMessages);
    }

}
