package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Cache.CacheService;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfig;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserModelAccessService {

    @Autowired
    private UserModelAccessConfigRepository userModelAccessConfigRepository;

    @Autowired
    private chatServicesManageService chatServicesManageService;

    @Autowired
    private CacheService cacheService;

    /**
     * Obtain the model services that the user is allowed to access.
     */
    public List<ChatService> getUserAllowedChatServices(String userId) {
        Optional<UserModelAccessConfig> accessConfigOpt = getAccessConfigFromCache(userId);

        if (accessConfigOpt.isPresent()) {
            try {
                UserModelAccessConfig accessConfig = accessConfigOpt.get();
                List<UserModelAccessConfig.ModelAccess> allowedModels = accessConfig.getAllowedModels();

                // Filter the user-allowed model services from the global chatServices.
                return allowedModels.stream()
                    .map(modelAccess -> chatServicesManageService.getChatServices()
                        .get(modelAccess.getUrl())
                        .get(modelAccess.getModel()))
                    .filter(Objects::nonNull) // Filter out nonexistent services.
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Failed to get user allowed chat services", e);
                throw new RuntimeException(e);
            }
        } else {
            throw new AccessDeniedException("No access configuration found for user " + userId);
        }
    }

    public List<UserModelAccessConfig.ModelAccess> getUserAllowedModelAccess(String userId) {
        Optional<UserModelAccessConfig> accessConfigOpt = getAccessConfigFromCache(userId);

        if (accessConfigOpt.isPresent()) {
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            return accessConfig.getAllowedModels();
        } else {
            throw new AccessDeniedException("No access configuration found for user " + userId);
        }
    }

    public void updateUserAccessConfig(String userId, List<UserModelAccessConfig.ModelAccess> newModelAccess) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessConfigRepository.findById(userId);

        // If the user's configuration is found, update it.
        if (accessConfigOpt.isPresent()) {
            log.info("Updating user access config for user {}", userId);
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            accessConfig.setAllowedModels(newModelAccess);
            userModelAccessConfigRepository.save(accessConfig);
        }
        // If the user's configuration cannot be found, create a new configuration.
        else {
            log.info("Creating new user access config for user {}", userId);
            UserModelAccessConfig newAccessConfig = new UserModelAccessConfig();
            newAccessConfig.setUserId(userId);
            newAccessConfig.setAllowedModels(newModelAccess);
            userModelAccessConfigRepository.save(newAccessConfig);
        }
        
        // 清除该用户的模型访问配置缓存
        cacheService.clearUserModelAccessCache(userId);
    }

    public void grantAllAvailiableModels(String userId) {
        List<UserModelAccessConfig.ModelAccess> allModelAccess = new ArrayList<>();
        for (String serviceName : chatServicesManageService.getChatServices().keySet()) {
            for (String modelId : chatServicesManageService.getChatServices().get(serviceName).keySet()) {
                allModelAccess.add(new UserModelAccessConfig.ModelAccess(serviceName, modelId));
            }
        }
        updateUserAccessConfig(userId, allModelAccess);
    }

    /**
     * 从缓存获取用户访问配置
     */
    @Cacheable(value = "user_model_access", key = "#userId", unless = "#result.isEmpty()")
    public Optional<UserModelAccessConfig> getAccessConfigFromCache(String userId) {
        log.info("从MongoDB获取用户模型访问配置: userId={}", userId);
        return userModelAccessConfigRepository.findById(userId);
    }
}
