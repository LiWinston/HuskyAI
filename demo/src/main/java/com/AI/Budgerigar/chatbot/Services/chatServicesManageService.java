package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Config.RemoteServiceConfig;
import com.AI.Budgerigar.chatbot.Services.Factory.OpenAIChatServiceFactory;
import com.AI.Budgerigar.chatbot.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j

public class chatServicesManageService {

    // Use a double-layer Map: the outer Map uses the service's URL or alias as the key,
    // and the inner Map uses the model ID as the key.
    @Autowired
    @Getter
    private ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices;

    @Autowired
    @Qualifier("baidu")
    private ChatService baiduChatService;

    @Autowired
    @Qualifier("doubao")
    private ChatService doubaoChatService;

    @Autowired
    private RemoteServiceConfig remoteServiceConfig;

    @Autowired
    private OpenAIChatServiceFactory openAIChatServiceFactory;

    @Autowired
    private ExecutorService executorService;// thread pool

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
        // 从配置中读取根路径并动态注册服务
        for (RemoteServiceConfig.ServiceConfig service : remoteServiceConfig.getServiceConfigs()) {
            fetchAndRegisterModelsFromService(service);
        }
    }

    /**
     * Obtain the model from the remote service and register it at @serviceName. If an
     * alias is set, use the alias as the key; otherwise, use the service's URL.
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

                // Get a list of models permitted for registration.
                List<String> allowedModels = serviceConfig.getAllowedModels();

                for (Map<String, Object> modelData : models) {
                    String modelId = (String) modelData.get("id");

                    // If allowedModels is not empty and modelId is not in the list, skip.
                    if (allowedModels != null && !allowedModels.isEmpty() && !allowedModels.contains(modelId)) {
                        log.info("Model {} is not in the allowed list for service {}, skipping registration.", modelId,
                                serviceName);
                        continue;
                    }
                    // If the model does not exist, register a new ChatService.
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

    // @Async
    @Scheduled(fixedDelayString = "${chatbot.model.health.refresh.rate}")
    @SchedulerLock(name = "checkRemoteServicesHealth", lockAtMostFor = "15s", lockAtLeastFor = "15s")
    public void autoRefresh() {
        checkRemoteServicesHealth().thenAccept(result -> {
            if (result.getCode() == 1) {
                AbstractMap.SimpleEntry<Integer, Integer> modelChanges = result.getData();
                log.info("Auto-refreshed remote services health. Added {} models, removed {} models.",
                        modelChanges.getKey(), modelChanges.getValue());
            }
            else {
                log.error("Failed to auto-refresh remote services health: {}", result.getMsg());
            }
        });
    }

    public CompletableFuture<Result<AbstractMap.SimpleEntry<Integer, Integer>>> checkRemoteServicesHealth() {
        return CompletableFuture.supplyAsync(() -> {
            int modelsAdded = 0;
            int modelsRemoved = 0;
            StringBuilder logMessage = new StringBuilder("Checking remote services health...\n");

            for (RemoteServiceConfig.ServiceConfig serviceConfig : remoteServiceConfig.getServiceConfigs()) {
                String serviceUrl = serviceConfig.getUrl();
                String serviceName = serviceConfig.getName() != null ? serviceConfig.getName() : serviceUrl;
                String openaiApiKey = serviceConfig.getApiKey() != null ? serviceConfig.getApiKey() : "";

                // Fetch available models from the remote service
                List<String> availableModels = fetchModelsFromService(serviceUrl, serviceConfig);
                // logMessage.append(String.format("Available models from %s: %s %s",
                // serviceName, availableModels, System.lineSeparator()));

                // Get the currently registered models for this service
                ConcurrentHashMap<String, ChatService> registeredModels = chatServices.getOrDefault(serviceName,
                        new ConcurrentHashMap<>());

                // Register new models
                for (String modelId : availableModels) {
                    if (!registeredModels.containsKey(modelId)) {
                        registerNewChatService(modelId, serviceUrl, serviceName, openaiApiKey);
                        logMessage.append(String.format("Registered new model %s from %s %s", modelId, serviceName,
                                System.lineSeparator()));
                        modelsAdded++; // Increment count of added models
                    }
                }

                // Remove models that are no longer available
                for (String modelId : registeredModels.keySet()) {
                    if (!availableModels.contains(modelId)) {
                        registeredModels.remove(modelId);
                        logMessage.append(String.format("Removing model %s from %s %s", modelId, serviceName,
                                System.lineSeparator()));
                        modelsRemoved++; // Increment count of removed models
                    }
                }

                // Update the registered models for this service
                chatServices.put(serviceName, registeredModels);
            }

            // Return a SimpleEntry containing the number of added and removed models
            return Result.success(new AbstractMap.SimpleEntry<>(modelsAdded, modelsRemoved), logMessage.toString());
        }, executorService);
    }

    private List<String> fetchModelsFromService(String serviceUrl, RemoteServiceConfig.ServiceConfig serviceConfig) {
        try {
            String modelEndpoint = serviceUrl + "/v1/models";
            String apikey = remoteServiceConfig.getServiceConfigs()
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

            // Obtain model ID
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
            // Only record HTTP status codes and error messages.
            log.error("Failed to fetch models from service: {}, HTTP status: {}, Error message: {}", serviceUrl,
                    e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        }
        catch (Exception e) {
            // Catch other exceptions.
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
