package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import com.AI.Budgerigar.chatbot.Config.RemoteServiceConfig;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

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

    private final ConcurrentHashMap<String, ChatService> chatServices = new ConcurrentHashMap<>();

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        chatServices.put("baidu", baiduChatService);
        chatServices.put("doubao", doubaoChatService);
        chatServices.put("openai", openaiChatService);
        // 从配置中读取根路径并动态注册服务
        for (RemoteServiceConfig.ServiceConfig service : remoteServiceConfig.getServices()) {
            fetchAndRegisterModelsFromService(service.getUrl());
        }
    }

    private void fetchAndRegisterModelsFromService(String baseUrl) {
        String modelsEndpoint = baseUrl + "/v1/models";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(modelsEndpoint, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.getBody().get("data");
                for (Map<String, Object> modelData : models) {
                    String modelId = (String) modelData.get("id");
                    registerNewChatService(modelId, baseUrl);
                }
            }
            else {
                log.warn("Failed to fetch models from {}: {}", modelsEndpoint, response.getStatusCode());
            }
        }
        catch (Exception e) {
            log.error("Error fetching models from {}", modelsEndpoint, e);
        }
    }

    @Scheduled(fixedDelay = 60000)  // 每60秒执行一次
    public void checkRemoteServicesHealth() {
        executorService.submit(() -> {
            chatServices.entrySet().removeIf(entry -> {
                String serviceName = entry.getKey();
                if (!"baidu".equals(serviceName) && !"doubao".equals(serviceName) && !"openai".equals(serviceName)) {
                    ChatService service = entry.getValue();
                    if(service.getClass() != OpenAIChatServiceImpl.class) {
                        return false;
                    }
                    OpenAIChatServiceImpl Oservice = (OpenAIChatServiceImpl) service;
                    // 检查服务是否可用
                    if (!isServiceAvailable(Oservice)) {
                        log.info("Service {} is unavailable, removing from chatServices", serviceName);
                        return true;  // 移除不可用服务
                    }
                }
                return false;
            });
        });
    }

    private boolean isServiceAvailable(OpenAIChatServiceImpl service) {
        try {
            // 从 ChatService 实例中获取根路径，拼接 models 端点
            String chatUrl = service.getOpenAIUrl();
            //"/v1/chat/completion" -> "/v1/models"
            String checkUrl = chatUrl.replace("/v1/chat/completion", "/v1/models");

            // 使用 RestTemplate 发送 GET 请求来检查服务状态
            ResponseEntity<String> response = restTemplate.getForEntity(checkUrl, String.class);

            // 检查是否返回成功的 2xx 状态码
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // 如果发生异常，说明服务不可用，返回 false
            log.error("Service check failed for {}: {}", service.getOpenAIUrl() + "-" + service.getModel(), e.getMessage());
            return false;
        }
    }

    private void registerNewChatService(String modelId, String baseUrl) {
        ChatService newService = OpenAIChatServiceImpl.create(baseUrl + "/v1/chat/completion", modelId);
        chatServices.put(modelId, newService);
        log.info("Registered new ChatService with model: {} from {}", modelId, baseUrl + "/v1/chat/completion");
    }

    @Autowired
    private userService userService;

    @Autowired
    private ChatSyncService chatSyncService;

    @Autowired
    private ExecutorService executorService;// 线程池 thread pool

    @Autowired
    private ConversationMapper conversationMapper;

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
    public Result<?> chatPost(@RequestBody Map<String, String> body) {
        try {
            // 使用 chatService 的 chat 方法并返回结果
            String model = body.get("model");
            if (model == null) {
                model = "baidu";
            }
            ChatService chatService = chatServices.get(model);
            Result<String> response = chatService.chat(body.get("prompt"), body.get("conversationId"));
            return Result.success(response.getData(), response.getMsg());
        }
        catch (Exception e) {
            // 捕获任何异常并返回错误响应
            return Result.error(e.getMessage());
        }
    }

    @RequestMapping(value = "/chat", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptionsRequest() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/models")
    public Result<?> getModels() {
        try {
            return Result.success(chatServices.keySet());
        }
        catch (Exception e) {
            return Result.error(e.getMessage());
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
