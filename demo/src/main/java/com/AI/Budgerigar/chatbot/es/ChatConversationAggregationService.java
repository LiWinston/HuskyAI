package com.AI.Budgerigar.chatbot.es;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatConversationAggregationService {

    @Autowired
    private RestHighLevelClient client;

    // 获取前 n 个热搜关键词
    public List<String> getTopKeywords(int n) throws IOException {
        // 构建 Elasticsearch 搜索请求
        SearchRequest searchRequest = new SearchRequest("chat_conversations");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 添加聚合，用于统计 messages 字段中的最常见词汇
        sourceBuilder.aggregation(AggregationBuilders.terms("top_keywords").field("messages").size(n));
        searchRequest.source(sourceBuilder);

        // 执行搜索并解析响应
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Terms terms = searchResponse.getAggregations().get("top_keywords");

        // 返回前 n 个热词
        return terms.getBuckets().stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());
    }

}
