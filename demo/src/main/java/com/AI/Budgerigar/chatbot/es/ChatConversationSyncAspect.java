package com.AI.Budgerigar.chatbot.es;

import com.AI.Budgerigar.chatbot.Entity.Message;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
public class ChatConversationSyncAspect {

    @Autowired
    private ChatConversationRecordESRepository esRepository;

    // 拦截 updateHistoryById 方法并在执行后同步数据到 Elasticsearch
    @AfterReturning(pointcut = "execution(* com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAOImpl.updateHistoryById(..))", returning = "newMessages")
    public void syncUpdateToElasticsearch(Object[] newMessages) {
        String conversationId = (String) newMessages[0];  // 获取 conversationId
        List<Message> messages = (List<Message>) newMessages[1];  // 获取消息列表

        // 构建 Elasticsearch 对象并同步
        ChatConversationRecordES esRecord = new ChatConversationRecordES(conversationId, convertMessagesToArray(messages));
        esRepository.save(esRecord);
    }

    // 拦截 replaceHistoryById 方法并在执行后同步数据到 Elasticsearch
    @AfterReturning(pointcut = "execution(* com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAOImpl.replaceHistoryById(..))", returning = "newMessages")
    public void syncReplaceToElasticsearch(Object[] newMessages) {
        String conversationId = (String) newMessages[0];  // 获取 conversationId
        List<Message> messages = (List<Message>) newMessages[1];  // 获取消息列表

        // 构建 Elasticsearch 对象并同步
        ChatConversationRecordES esRecord = new ChatConversationRecordES(conversationId, convertMessagesToArray(messages));
        esRepository.save(esRecord);
    }

    // 拦截 deleteConversationById 方法并在执行后从 Elasticsearch 删除记录
    @AfterReturning(pointcut = "execution(* com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAOImpl.deleteConversationById(..))", returning = "conversationId")
    public void syncDeleteFromElasticsearch(Object conversationId) {
        // 从 Elasticsearch 中删除记录
        esRepository.deleteById((String) conversationId);
    }

    // Helper 方法，将 List<Message> 转换为 List<String[]>
    private List<String[]> convertMessagesToArray(List<Message> messages) {
        return messages.stream()
                .map(msg -> new String[] { msg.getRole(), msg.getTimestamp(), msg.getContent() })
                .collect(Collectors.toList());
    }
}
