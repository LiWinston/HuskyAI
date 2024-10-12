package com.AI.Budgerigar.chatbot.chatbotAdmin.Controller;

import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.es.ChatConversationRecordES;
import com.AI.Budgerigar.chatbot.es.ChatConversationRecordESRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatConversationSearchService {

    @Autowired
    private ChatConversationRecordESRepository esRepository;

    // 根据关键词搜索对话，并封装为 List<Message>
    public List<Message> searchByKeyword(String keyword) {
        List<ChatConversationRecordES> records = esRepository.findByMessagesContaining(keyword);

        // 提取搜索到的对话片段并封装成 List<Message>
        List<Message> searchedMessages = new ArrayList<>();
        for (ChatConversationRecordES record : records) {
            List<Message> filteredMessages = record.getMessages()
                .stream()
                .filter(msg -> msg[2].contains(keyword)) // 过滤包含关键词的消息
                .map(msg -> new Message(msg[0], msg[1], msg[2])) // 转换为 Message 对象
                .collect(Collectors.toList());
            searchedMessages.addAll(filteredMessages);
        }
        return searchedMessages;
    }

}