package com.AI.Budgerigar.chatbot.Nosql;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserModelAccessConfigRepository extends MongoRepository<UserModelAccessConfig, String> {

    // Automatically convert to a query to find the configuration of a specific URL.
    List<UserModelAccessConfig> findByAllowedModelsUrl(String url);

    UserModelAccessConfig findByUserIdAndAllowedModelsUrlAndAllowedModelsModel(String userId, String url, String model);

}