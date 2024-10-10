package com.AI.Budgerigar.chatbot.Services;

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

    /**
     * 获取用户允许访问的模型服务
     */
    public List<ChatService> getUserAllowedChatServices(String userId) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessConfigRepository.findById(userId);

        if (accessConfigOpt.isPresent()) {
            try {
                UserModelAccessConfig accessConfig = accessConfigOpt.get();
                List<UserModelAccessConfig.ModelAccess> allowedModels = accessConfig.getAllowedModels();

                // 从全局的 chatServices 中筛选出用户允许的模型服务
                return allowedModels.stream()
                    .map(modelAccess -> chatServicesManageService.getChatServices()
                        .get(modelAccess.getUrl())
                        .get(modelAccess.getModel()))
                    .filter(Objects::nonNull) // 过滤掉不存在的服务
                    .collect(Collectors.toList());
            }
            catch (Exception e) {
                log.error("Failed to get user allowed chat services", e);
                throw new RuntimeException(e);
            }
        }
        else {
            throw new AccessDeniedException("No access configuration found for user " + userId);
        }
    }

    public List<UserModelAccessConfig.ModelAccess> getUserAllowedModelAccess(String userId) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessConfigRepository.findById(userId);

        if (accessConfigOpt.isPresent()) {
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            return accessConfig.getAllowedModels();
        }
        else {
            throw new AccessDeniedException("No access configuration found for user " + userId);
        }
    }

    public void updateUserAccessConfig(String userId, List<UserModelAccessConfig.ModelAccess> newModelAccess) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessConfigRepository.findById(userId);

        // 如果找到用户的配置，则更新
        if (accessConfigOpt.isPresent()) {
            log.info("Updating user access config for user {}", userId);
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            accessConfig.setAllowedModels(newModelAccess);
            userModelAccessConfigRepository.save(accessConfig);
        }
        // 如果找不到用户的配置，创建新的配置
        else {
            log.info("Creating new user access config for user {}", userId);
            UserModelAccessConfig newAccessConfig = new UserModelAccessConfig();
            newAccessConfig.setUserId(userId);
            newAccessConfig.setAllowedModels(newModelAccess);
            userModelAccessConfigRepository.save(newAccessConfig);
        }
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

    // public boolean isModelAllowed(String userId, String url, String model) {
    // return repository.findByUserIdAndAllowedModelsUrlAndAllowedModelsModel(userId, url,
    // model) != null;
    // }
    //
    // public List<UserModelAccessConfig.ModelAccess> getAllowedModels(String userId) {
    // UserModelAccessConfig config = repository.findById(userId).orElse(null);
    // return config == null ? List.of() : config.getAllowedModels();
    // }

}
