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

    // Save share records.
    public void saveShare(String shareCode, String uuid, String conversationId, List<Integer> messageIndexes) {
        ShareRecord shareRecord = ShareRecord.builder()
            .shareCode(shareCode)
            .uuid(uuid)
            .conversationId(conversationId)
            .messageIndexes(messageIndexes)
            .build();
        mongoTemplate.save(shareRecord);
    }

    // Query sharing records based on the sharing code.
    public ShareRecord findByShareCode(String shareCode) {
        Query query = new Query(Criteria.where("shareCode").is(shareCode));
        return mongoTemplate.findOne(query, ShareRecord.class);
    }

    // Query the list of shared conversationIds based on UUID.
    public List<String> findConversationIdsByUuid(String uuid) {
        Query query = new Query(Criteria.where("uuid").is(uuid));
        return mongoTemplate.find(query, ShareRecord.class)
            .stream()
            .map(ShareRecord::getConversationId)
            .collect(Collectors.toList());
    }

    // Delete sharing record based on sharing code.
    public void deleteByShareCode(String shareCode) {
        Query query = new Query(Criteria.where("shareCode").is(shareCode));
        mongoTemplate.remove(query, ShareRecord.class);
    }

}
