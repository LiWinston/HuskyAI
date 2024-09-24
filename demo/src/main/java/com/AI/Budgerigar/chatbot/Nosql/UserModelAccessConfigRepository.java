package com.AI.Budgerigar.chatbot.Nosql;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserModelAccessConfigRepository extends MongoRepository<UserModelAccessConfig, String> {

    // 这个方法会自动转换为查询，查找特定URL的配置
    List<UserModelAccessConfig> findByAllowedModelsUrl(String url);

    UserModelAccessConfig findByUserIdAndAllowedModelsUrlAndAllowedModelsModel(String userId, String url, String model);

}