package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Exceptions.ResourceNotFoundException;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfig;
import com.AI.Budgerigar.chatbot.Nosql.UserModelAccessConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserModelAccessService {

    @Autowired
    private UserModelAccessConfigRepository userModelAccessConfigRepository;

    @Autowired
    @Qualifier("chatServices")
    private ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices;

    /**
     * 获取用户允许访问的模型服务
     */
    public List<ChatService> getUserAllowedChatServices(String userId) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessConfigRepository.findById(userId);

        if (accessConfigOpt.isPresent()) {
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            List<UserModelAccessConfig.ModelAccess> allowedModels = accessConfig.getAllowedModels();

            // 从全局的 chatServices 中筛选出用户允许的模型服务
            return allowedModels.stream()
                .map(modelAccess -> chatServices.get(modelAccess.getUrl()).get(modelAccess.getModel()))
                .filter(Objects::nonNull) // 过滤掉不存在的服务
                .collect(Collectors.toList());
        }
        else {
            throw new AccessDeniedException("No access configuration found for user " + userId);
        }
    }

    /**
     * 更新用户的模型访问权限
     */
    public void updateUserAccessConfig(String userId, List<UserModelAccessConfig.ModelAccess> newModelAccess) {
        Optional<UserModelAccessConfig> accessConfigOpt = userModelAccessConfigRepository.findById(userId);

        if (accessConfigOpt.isPresent()) {
            UserModelAccessConfig accessConfig = accessConfigOpt.get();
            accessConfig.setAllowedModels(newModelAccess);
            userModelAccessConfigRepository.save(accessConfig);
        }
        else {
            throw new ResourceNotFoundException("User config not found for user " + userId);
        }
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
