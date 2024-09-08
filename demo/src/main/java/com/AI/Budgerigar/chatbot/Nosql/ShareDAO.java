package com.AI.Budgerigar.chatbot.Nosql;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ShareDAO {

    private final MongoTemplate mongoTemplate;

    public ShareDAO(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // 保存分享记录
    public void saveShare(String shareCode, String uuid, String conversationId, List<Integer> messageIndexes) {
        ShareRecord shareRecord = ShareRecord.builder()
            .shareCode(shareCode)
            .uuid(uuid)
            .conversationId(conversationId)
            .messageIndexes(messageIndexes)
            .build();
        mongoTemplate.save(shareRecord);
    }

    // 根据分享码查询分享记录
    public ShareRecord findByShareCode(String shareCode) {
        Query query = new Query(Criteria.where("shareCode").is(shareCode));
        return mongoTemplate.findOne(query, ShareRecord.class);
    }

    // 根据 UUID 查询分享的 conversationId 列表
    public List<String> findConversationIdsByUuid(String uuid) {
        Query query = new Query(Criteria.where("uuid").is(uuid));
        return mongoTemplate.find(query, ShareRecord.class)
            .stream()
            .map(ShareRecord::getConversationId)
            .collect(Collectors.toList());
    }

    // 根据分享码删除分享记录
    public void deleteByShareCode(String shareCode) {
        Query query = new Query(Criteria.where("shareCode").is(shareCode));
        mongoTemplate.remove(query, ShareRecord.class);
    }

}
