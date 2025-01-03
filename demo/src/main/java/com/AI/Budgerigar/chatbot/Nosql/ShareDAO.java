package com.AI.Budgerigar.chatbot.Nosql;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ShareDAO {

    private final MongoTemplate mongoTemplate;

    public ShareDAO(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // Save share records with expiration time
    public void saveShare(String shareCode, String uuid, String conversationId, List<Integer> messageIndexes, Date expireAt) {
        ShareRecord shareRecord = ShareRecord.builder()
            .shareCode(shareCode)
            .uuid(uuid)
            .conversationId(conversationId)
            .messageIndexes(messageIndexes)
            .expireAt(expireAt)
            .createdAt(new Date())
            .build();
        mongoTemplate.save(shareRecord);
    }

    // Query sharing records based on the sharing code
    public ShareRecord findByShareCode(String shareCode) {
        Query query = new Query(Criteria.where("shareCode").is(shareCode));
        return mongoTemplate.findOne(query, ShareRecord.class);
    }

    // Query all share records by UUID
    public List<ShareRecord> findAllByUuid(String uuid) {
        Query query = new Query(Criteria.where("uuid").is(uuid));
        return mongoTemplate.find(query, ShareRecord.class);
    }

    // Query the list of shared conversationIds based on UUID
    public List<String> findConversationIdsByUuid(String uuid) {
        Query query = new Query(Criteria.where("uuid").is(uuid));
        return mongoTemplate.find(query, ShareRecord.class)
            .stream()
            .map(ShareRecord::getConversationId)
            .collect(Collectors.toList());
    }

    // Delete sharing record based on sharing code
    public void deleteByShareCode(String shareCode) {
        Query query = new Query(Criteria.where("shareCode").is(shareCode));
        mongoTemplate.remove(query, ShareRecord.class);
    }

    // Delete all sharing records for a conversation
    public void deleteByConversationId(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, ShareRecord.class);
    }

    // Update share expiration time
    public void updateExpireAt(String shareCode, Date newExpireAt) {
        Query query = new Query(Criteria.where("shareCode").is(shareCode));
        Update update = new Update().set("expireAt", newExpireAt);
        mongoTemplate.updateFirst(query, update, ShareRecord.class);
    }
}
