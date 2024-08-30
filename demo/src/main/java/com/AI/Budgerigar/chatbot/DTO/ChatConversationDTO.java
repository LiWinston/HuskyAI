package com.AI.Budgerigar.chatbot.DTO;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Document(collection = "chat_conversations")
@Setter
public class ChatConversationDTO {

    // Getter 和 Setter 方法
    @Setter
    @Id
    // 对话ID, 用于mongo唯一搜索
    private String conversationId;

    // 对话记录，每条消息是一个String数组，第一个元素是说话者，第二个元素是消息内容
    private List<String[]> messages;

    // 无参构造函数，用于MongoDB的反序列化
    public ChatConversationDTO() {
        this.messages = new ArrayList<>();
    }

    // 构造函数，用于手动创建ChatConversation对象
    public ChatConversationDTO(String conversationId, List<String[]> messages) {
        this.conversationId = conversationId;
        this.messages = messages;
    }

    public void setMessages(List<String[]> messages) {
        this.messages = messages;
    }

    // 方法用于添加单条消息到对话记录中
    public void addMessage(String message) {
        try {
            String[] speakerAndMessage = message.split(":", 2);
            messages.add(speakerAndMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addMessage(String speaker, String message) {
        messages.add(new String[]{speaker, message});
    }

    public void addMessage(List<String[]> messages) {
        this.messages.addAll(messages);
    }

    // toString 方法用于输出对象信息
    @Override
    public String toString() {
        return "ChatConversationDTO{" +
                "conversationId='" + conversationId + '\'' +
                ", messages=" + messages +
                '}';
    }
}