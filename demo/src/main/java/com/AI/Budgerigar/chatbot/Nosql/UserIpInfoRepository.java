package com.AI.Budgerigar.chatbot.Nosql;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserIpInfoRepository extends MongoRepository<UserIpInfo, String> {
    Optional<UserIpInfo> findByUserUuid(String userUuid);
}
