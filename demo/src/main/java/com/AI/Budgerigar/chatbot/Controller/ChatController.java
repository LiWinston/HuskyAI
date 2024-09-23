package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import com.AI.Budgerigar.chatbot.Config.RemoteServiceConfig;
import com.AI.Budgerigar.chatbot.Constant.ApplicationConstant;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.Services.Factory.OpenAIChatServiceFactory;
import com.AI.Budgerigar.chatbot.Services.StreamChatService;
import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    // 使用双层 Map：外层 Map 以服务的 URL 或别名作为 key，内层 Map 以模型 ID 作为 key
    @Autowired
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
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationConstant applicationConstant;

    @Autowired
    private OpenAIChatServiceFactory openAIChatServiceFactory;

    @Autowired
    private userService userService;

    @Autowired
    private ChatSyncService chatSyncService;

    @Autowired
    private ExecutorService executorService;// 线程池 thread pool

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ObjectMapper objectMapper;

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

    private void fetchAndRegisterModelsFromService(RemoteServiceConfig.ServiceConfig serviceConfig) {
        String baseUrl = serviceConfig.getUrl();
        String modelsEndpoint = baseUrl + "/v1/models";
        String serviceName = serviceConfig.getName() != null ? serviceConfig.getName() : baseUrl;
        String openaiApiKey = serviceConfig.getApiKey() != null ? serviceConfig.getApiKey() : "";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(modelsEndpoint, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.getBody().get("data");
                ConcurrentHashMap<String, ChatService> modelServices = chatServices.getOrDefault(serviceName,
                        new ConcurrentHashMap<>());
                for (Map<String, Object> modelData : models) {
                    String modelId = (String) modelData.get("id");
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

    // @Scheduled(fixedDelay = 600000) // check every 10 minutes
    public void checkRemoteServicesHealth() {
        executorService.submit(() -> {
            for (RemoteServiceConfig.ServiceConfig serviceConfig : remoteServiceConfig.getServices()) {
                String serviceUrl = serviceConfig.getUrl();
                String serviceName = serviceConfig.getName() != null ? serviceConfig.getName() : serviceUrl;
                String openaiApiKey = serviceConfig.getApiKey() != null ? serviceConfig.getApiKey() : "";

                List<String> availableModels = fetchModelsFromService(serviceUrl);
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

    private List<String> fetchModelsFromService(String serviceUrl) {
        try {
            String modelEndpoint = serviceUrl + "/v1/models";
            ResponseEntity<String> response = restTemplate.getForEntity(modelEndpoint, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return Collections.emptyList();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            List<String> modelIds = new ArrayList<>();
            JsonNode dataNode = jsonResponse.get("data");
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode modelNode : dataNode) {
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

    private boolean isServiceAvailable(OpenAIChatServiceImpl service) {
        try {
            String chatUrl = service.getOpenAIUrl();
            String checkUrl = chatUrl.replace("/v1/chat/completion", "/v1/models");

            ResponseEntity<String> response = restTemplate.getForEntity(checkUrl, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return false;
            }

            // 使用 Jackson 解析 JSON 响应
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            // 检查 "data" 数组中是否包含指定的模型 ID
            JsonNode dataNode = jsonResponse.get("data");
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode modelNode : dataNode) {
                    if (modelNode.get("id").asText().equals(service.getModel())) {
                        return true; // 模型存在，服务可用
                    }
                }
            }

            // 如果模型未找到，返回 false
            log.info("Model {} not found in service {}. Service might be unavailable.", service.getModel(),
                    service.getOpenAIUrl());
            return false;

        }
        catch (Exception e) {
            log.error("Service check failed for {}: {}", service.getOpenAIUrl() + "-" + service.getModel(),
                    e.getMessage());
            return false;
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

    // 获取DB中所有对话清单，以备用户选取.每个对话清单包含对话ID和对话节选
    // get all conversation list from DB for user to choose.
    // Each conversation list contains conversation ID and conversation excerpt
    @GetMapping()
    public Result<?> getConversationList(@RequestParam String uuid) {
        try {
            // 使用 userService 查询用户是否存在
            var userExists = userService.checkUserExistsByUuid(uuid);
            if (userExists.getCode() == 0) {
                return Result.error(userExists.getMsg());
            }

            // 获取用户的对话列表及消息节选
            List<Conversation> conversations = userService.getConversations(uuid);
            return Result.success(conversations);

        }
        catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 用get传输ConversationId，表达获取历史记录之义
    // use get to transfer ConversationId, express the meaning of getting history
    // restfully
    @GetMapping("/{uuid}/{conversationId}")
    public Result<?> chat(@PathVariable String uuid, @PathVariable String conversationId) {
        if (!conversationMapper.checkConversationExistsByUuid(uuid, conversationId)) {
            log.info("conversationId not exists, create new conversationId");
            try {
                conversationMapper.createConversationForUuid(uuid, conversationId);
            }
            catch (Exception e) {
                return Result.error(e.getMessage());
            }
        }
        // 先把当前对话缓存提交到DB: first submit current conversation cache to DB
        // executorService.submit(() ->
        // chatSyncService.updateHistoryFromRedis(chatService.getConversationId()));
        chatSyncService.updateHistoryFromRedis(conversationId);
        // 读取 ConversationId 并设置到 chatService 中: read ConversationId and set to

        chatSyncService.updateRedisFromMongo(conversationId);
        // get历史传给前端显示: get history to show in front end
        try {
            List<Message> messageList = chatSyncService.getHistory(conversationId);
            return Result.success(messageList);
        }
        catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<String> chatPost(@RequestBody Map<String, String> body) {
        try {
            String model = body.get("model");
            if (model == null) {
                model = "baidu";
            }
            String[] modelParts = model.split("上的");
            if (modelParts.length != 2) {
                return Result.error("Invalid model format. Expected serviceName:modelId");
            }

            String serviceName = modelParts[0];
            String modelId = modelParts[1];

            ConcurrentHashMap<String, ChatService> serviceModels = chatServices.get(serviceName);
            if (serviceModels == null) {
                return Result.error("Service not found: " + serviceName);
            }

            ChatService chatService = serviceModels.get(modelId);

            if (chatService == null) {
                return Result.error("Model not found: " + model);
            }
            chatServices.forEach((k, v) -> {
                log.info("key: " + k + " value: " + v.toString());
            });

            // 非流式调用
            Result<String> response = chatService.chat(body.get("prompt"), body.get("conversationId"));
            return Result.success(response.getData(), response.getMsg());
        }
        catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/stream")
    public Flux<String> chatPostStream(@RequestBody Map<String, String> body) {
        try {
            String model = body.get("model");
            if (model == null) {
                model = "baidu上的baidu";
            }

            String[] modelParts = model.split("上的");
            if (modelParts.length != 2) {
                return Flux.just(Result.error("Invalid model format. Expected serviceName:modelId")).map(result -> {
                    try {
                        return objectMapper.writeValueAsString(result) + "\n";
                    }
                    catch (JsonProcessingException e) {
                        throw new RuntimeException("Error serializing result", e);
                    }
                });
            }

            String serviceName = modelParts[0];
            String modelId = modelParts[1];

            ConcurrentHashMap<String, ChatService> serviceModels = chatServices.get(serviceName);
            if (serviceModels == null) {
                return Flux.just(Result.error("Service not found: " + serviceName)).map(result -> {
                    try {
                        return objectMapper.writeValueAsString(result) + "\n";
                    }
                    catch (JsonProcessingException e) {
                        throw new RuntimeException("Error serializing result", e);
                    }
                });
            }

            ChatService chatService = serviceModels.get(modelId);
            if (chatService == null) {
                return Flux.just(Result.error("Model not found: " + modelId + " in service: " + serviceName))
                    .map(result -> {
                        try {
                            return objectMapper.writeValueAsString(result) + "\n";
                        }
                        catch (JsonProcessingException e) {
                            throw new RuntimeException("Error serializing result", e);
                        }
                    });
            }

            // 检查是否支持流式调用
            if (chatService instanceof StreamChatService) {
                log.info("支持流式调用");
                return ((StreamChatService) chatService).chatFlux(body.get("prompt"), body.get("conversationId"))
                    .map(result -> {
                        // 将Result对象转换为JSON字符串并添加换行符
                        try {
                            return objectMapper.writeValueAsString(result) + "\n";
                        }
                        catch (JsonProcessingException e) {
                            throw new RuntimeException("Error serializing result", e);
                        }
                    });
            }
            else {
                return Flux.just(Result.error("Model does not support streaming: " + model)).map(result -> {
                    try {
                        return objectMapper.writeValueAsString(result) + "\n";
                    }
                    catch (JsonProcessingException e) {
                        throw new RuntimeException("Error serializing result", e);
                    }
                });
            }
        }
        catch (Exception e) {
            return Flux.error(e);
        }
    }

    @RequestMapping(value = "/chat", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptionsRequest() {
        return ResponseEntity.ok().build();
    }

    // 保持前三个模型不变，并对其他模型按服务源内部的模型名排序
    @GetMapping("/models")
    public Result<?> getModels() {
        try {
            // 检查远程服务的健康状况
            checkRemoteServicesHealth();

            // 优先模型列表
            List<String> prioritizedModels = Arrays.asList("openai", "doubao", "baidu");
            // 初始化返回列表大小，避免不必要的扩容
            List<String> result = new ArrayList<>(chatServices.size());

            // 直接使用Stream过滤出优先模型，并按优先顺序排序
            prioritizedModels.stream().filter(chatServices::containsKey).forEach(serviceName -> {
                chatServices.get(serviceName).keySet().forEach(modelId -> {
                    result.add(serviceName + "上的" + modelId);
                });
            });

            // 对其余服务源的模型按模型名排序，并加入结果
            chatServices.keySet()
                .stream()
                .filter(serviceName -> !prioritizedModels.contains(serviceName))
                .forEach(serviceName -> {
                    chatServices.get(serviceName)
                        .keySet()
                        .stream()
                        .sorted()
                        .forEach(modelId -> result.add(serviceName + "上的" + modelId));
                });

            return Result.success(result);
        }
        catch (Exception e) {
            return Result.error(chatServices.keySet(), e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}/{conversationId}")
    public Result<?> deleteConversation(@PathVariable String uuid, @PathVariable String conversationId) {
        try {
            // 使用 userService 的 deleteConversation 方法并返回结果
            return chatSyncService.deleteConversation(uuid, conversationId);

        }
        catch (Exception e) {
            // 捕获任何异常并返回错误响应
            return Result.error(e.getMessage());
        }
    }

}
