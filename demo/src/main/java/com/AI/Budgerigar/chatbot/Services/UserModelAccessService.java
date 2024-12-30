package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Cache.CacheService;
import com.AI.Budgerigar.chatbot.Cache.UserModelAccessCacheService;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfig;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private UserModelAccessCacheService userModelAccessCacheService;

    /**
     * 获取用户允许访问的模型服务
     */
    public List<ChatService> getUserAllowedChatServices(String userId) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessCacheService.getAccessConfigFromCache(userId);

        if (accessConfigOpt.isPresent()) {
            try {
                UserModelAccessConfig accessConfig = accessConfigOpt.get();
                List<UserModelAccessConfig.ModelAccess> allowedModels = accessConfig.getAllowedModels();

                // 从全局chatServices中过滤出用户允许的模型服务
                return allowedModels.stream()
                    .map(modelAccess -> chatServicesManageService.getChatServices()
                        .get(modelAccess.getUrl())
                        .get(modelAccess.getModel()))
                    .filter(Objects::nonNull) // 过滤掉不存在的服务
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("获取用户允许的聊天服务失败", e);
                throw new RuntimeException(e);
            }
        } else {
            throw new AccessDeniedException("未找到用户 " + userId + " 的访问配置");
        }
    }

    public List<UserModelAccessConfig.ModelAccess> getUserAllowedModelAccess(String userId) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessCacheService.getAccessConfigFromCache(userId);

        if (accessConfigOpt.isPresent()) {
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            return accessConfig.getAllowedModels();
        } else {
            throw new AccessDeniedException("未找到用户 " + userId + " 的访问配置");
        }
    }

    public void updateUserAccessConfig(String userId, List<UserModelAccessConfig.ModelAccess> newModelAccess) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessConfigRepository.findById(userId);

        // 如果找到用户配置则更新
        if (accessConfigOpt.isPresent()) {
            log.info("更新用户访问配置: userId={}", userId);
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            accessConfig.setAllowedModels(newModelAccess);
            userModelAccessConfigRepository.save(accessConfig);
        }
        // 如果未找到用户配置则创建新配置
        else {
            log.info("创建新用户访问配置: userId={}", userId);
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
}
