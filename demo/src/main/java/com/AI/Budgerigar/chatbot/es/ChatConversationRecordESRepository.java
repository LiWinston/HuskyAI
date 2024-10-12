package com.AI.Budgerigar.chatbot.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatConversationRecordESRepository extends ElasticsearchRepository<ChatConversationRecordES, String> {

    // 自定义查询方法，根据关键词搜索
    List<ChatConversationRecordES> findByMessagesContaining(String keyword);

}
