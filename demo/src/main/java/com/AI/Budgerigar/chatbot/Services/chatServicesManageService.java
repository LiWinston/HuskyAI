package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Config.RemoteServiceConfig;
import com.AI.Budgerigar.chatbot.Services.Factory.OpenAIChatServiceFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j

public class chatServicesManageService {

    // 使用双层 Map：外层 Map 以服务的 URL 或别名作为 key，内层 Map 以模型 ID 作为 key
    @Autowired
    @Getter
    @Qualifier("chatServices")
    private ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices;

    @Autowired
    @Qualifier("baidu")
    private ChatService baiduChatService;

    @Autowired
    @Qualifier("doubao")
    private ChatService doubaoChatService;

    @Autowired
    @Qualifier("openai")
    private ChatService openaiChatService;

    @Autowired
    private RemoteServiceConfig remoteServiceConfig;

    @Autowired
    private OpenAIChatServiceFactory openAIChatServiceFactory;

    @Autowired
    private ExecutorService executorService;// 线程池 thread pool

    @PostConstruct
    public void init() {
        chatServices.put("baidu", new ConcurrentHashMap<>() {
            {
                put("baidu", baiduChatService);
            }
        });
        chatServices.put("doubao", new ConcurrentHashMap<>() {
            {
                put("doubao", doubaoChatService);
            }
        });
        chatServices.put("openai", new ConcurrentHashMap<>() {
            {
                put("openai", openaiChatService);
            }
        });
        // 从配置中读取根路径并动态注册服务
        for (RemoteServiceConfig.ServiceConfig service : remoteServiceConfig.getServices()) {
            fetchAndRegisterModelsFromService(service);
        }
    }

    /**
     * 从远程服务获取模型并注册 &#064;serviceName 若有别名设定则key使用别名，否则使用服务的 URL
     * @param serviceConfig
     */
    private void fetchAndRegisterModelsFromService(RemoteServiceConfig.ServiceConfig serviceConfig) {
        String baseUrl = serviceConfig.getUrl();
        String modelsEndpoint = baseUrl + "/v1/models";
        String serviceName = serviceConfig.getName() != null ? serviceConfig.getName() : baseUrl;
        String openaiApiKey = serviceConfig.getApiKey() != null ? serviceConfig.getApiKey() : "";

        try {
            var restTemplate = new RestTemplate() {
                {
                    getInterceptors().add((request, body, execution) -> {
                        request.getHeaders().add("Authorization", "Bearer " + openaiApiKey);
                        return execution.execute(request, body);
                    });
                }
            };

            ResponseEntity<Map> response = restTemplate.getForEntity(modelsEndpoint, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.getBody().get("data");
                ConcurrentHashMap<String, ChatService> modelServices = chatServices.getOrDefault(serviceName,
                        new ConcurrentHashMap<>());

                // 获取允许注册的模型列表
                List<String> allowedModels = serviceConfig.getAllowedModels();

                for (Map<String, Object> modelData : models) {
                    String modelId = (String) modelData.get("id");

                    // 如果 allowedModels 不为空，且 modelId 不在列表中，跳过
                    if (allowedModels != null && !allowedModels.isEmpty() && !allowedModels.contains(modelId)) {
                        log.info("Model {} is not in the allowed list for service {}, skipping registration.", modelId,
                                serviceName);
                        continue;
                    }
                    // 如果模型不存在，则注册新的 ChatService
                    if (!modelServices.containsKey(modelId)) {
                        registerNewChatService(modelId, baseUrl, serviceName, openaiApiKey);
                    }
                }
                chatServices.put(serviceName, modelServices);
            }
            else {
                log.warn("Failed to fetch models from {}: {}", modelsEndpoint, response.getStatusCode());
            }
        }
        catch (HttpServerErrorException e) {
            log.error("Failed to fetch models from {}: {} {}", modelsEndpoint, e.getStatusCode(),
                    e.getResponseBodyAsString());
        }
        catch (Exception e) {
            log.error("Failed to fetch models from {}: {}", modelsEndpoint, e.getMessage());
        }
    }

    @Async
    @Scheduled(fixedDelay = 600000) // check every 10 minutes
    @SchedulerLock(name = "checkRemoteServicesHealth", lockAtMostFor = "15s", lockAtLeastFor = "15s")
    public void checkRemoteServicesHealth() {
        executorService.submit(() -> {
            for (RemoteServiceConfig.ServiceConfig serviceConfig : remoteServiceConfig.getServices()) {
                String serviceUrl = serviceConfig.getUrl();
                String serviceName = serviceConfig.getName() != null ? serviceConfig.getName() : serviceUrl;
                String openaiApiKey = serviceConfig.getApiKey() != null ? serviceConfig.getApiKey() : "";

                List<String> availableModels = fetchModelsFromService(serviceUrl, serviceConfig);
                log.info("Available models from {}: {}", serviceName, availableModels);

                ConcurrentHashMap<String, ChatService> registeredModels = chatServices.getOrDefault(serviceName,
                        new ConcurrentHashMap<>());

                for (String modelId : availableModels) {
                    if (!registeredModels.containsKey(modelId)) {
                        registerNewChatService(modelId, serviceUrl, serviceName, openaiApiKey);
                    }
                }

                for (String modelId : registeredModels.keySet()) {
                    if (!availableModels.contains(modelId)) {
                        log.info("Model {} is no longer available in service {}, removing service.", modelId,
                                serviceName);
                        registeredModels.remove(modelId);
                    }
                }

                chatServices.put(serviceName, registeredModels);
            }
        });
    }

    private List<String> fetchModelsFromService(String serviceUrl, RemoteServiceConfig.ServiceConfig serviceConfig) {
        try {
            String modelEndpoint = serviceUrl + "/v1/models";
            String apikey = remoteServiceConfig.getServices()
                .stream()
                .filter(service -> service.getUrl().equals(serviceUrl))
                .findFirst()
                .get()
                .getApiKey();

            var restTemplate = new RestTemplate() {
                {
                    getInterceptors().add((request, body, execution) -> {
                        request.getHeaders().add("Authorization", "Bearer " + apikey);
                        return execution.execute(request, body);
                    });
                }
            };
            ResponseEntity<String> response = restTemplate.getForEntity(modelEndpoint, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return Collections.emptyList();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            List<String> allowedModels = serviceConfig.getAllowedModels();

            // 获取模型ID
            List<String> modelIds = new ArrayList<>();
            JsonNode dataNode = jsonResponse.get("data");
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode modelNode : dataNode) {
                    if (allowedModels != null && !allowedModels.isEmpty()
                            && !allowedModels.contains(modelNode.get("id").asText())) {
                        log.info("Model {} is not in the allowed list for service {}, skipping registration.",
                                modelNode.get("id").asText(), serviceUrl);
                        continue;
                    }
                    modelIds.add(modelNode.get("id").asText());
                }
            }
            return modelIds;
        }
        catch (HttpServerErrorException e) {
            // 只记录HTTP状态码和错误信息
            log.error("Failed to fetch models from service: {}, HTTP status: {}, Error message: {}", serviceUrl,
                    e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        }
        catch (Exception e) {
            // 捕获其他异常
            log.error("Failed to fetch models from service: {}", serviceUrl, e);
            return Collections.emptyList();
        }
    }

    private void registerNewChatService(String modelId, String baseUrl, String serviceName, String openaiApiKey) {
        ChatService newService = openAIChatServiceFactory.create(baseUrl + "/v1/chat/completions", modelId,
                openaiApiKey);
        ConcurrentHashMap<String, ChatService> modelServices = chatServices.getOrDefault(serviceName,
                new ConcurrentHashMap<>());
        modelServices.put(modelId, newService);
        chatServices.put(serviceName, modelServices);
        log.info("Registered new ChatService with model: {} from {}", modelId, serviceName);
    }

}
