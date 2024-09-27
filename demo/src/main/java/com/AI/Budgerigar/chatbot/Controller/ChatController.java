package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import com.AI.Budgerigar.chatbot.Services.*;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Autowired
    private userService userService;

    @Autowired
    private ChatSyncService chatSyncService;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private com.AI.Budgerigar.chatbot.Services.chatServicesManageService chatServicesManageService;

    @Autowired
    private UserModelAccessService userModelAccessService;

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

    /**
     * 用get传输ConversationId，表达获取历史记录之义
     * @param uuid 用户ID
     * @param conversationId 对话ID
     * @return 对话历史记录
     */
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
        var chatServices = chatServicesManageService.getChatServices();
        try {
            String model = body.get("model");
            if (model == null) {
                model = "baidu";
            }
            String[] modelParts = model.split("上的");
            if (modelParts.length != 2) {
                return Result.error("Invalid model format. Expected serviceName上的modelId");
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
        var chatServices = chatServicesManageService.getChatServices();
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
    @PostMapping("/models")
    public Result<?> getModels(@RequestBody Map<String, String> body) {
        String userUUID = body.get("uuid");
        // // 检查远程服务的健康状况
        // chatServicesManageService.checkRemoteServicesHealth();

        ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices = chatServicesManageService
            .getChatServices();
        try {
            // 优先模型列表
            List<String> prioritizedModels = Arrays.asList("HuskyGPT", "Aliyun", "baidu", "doubao", "PCLMStudio",
                    "MBP14LMStudio");

            List<ChatService> allowedChatServices = userModelAccessService.getUserAllowedChatServices(userUUID);
            log.info("Allowed chat services: " + allowedChatServices);

            // 最多也就这些了，还可能筛掉服务器现在不可用的服务
            List<String> result = new ArrayList<>(allowedChatServices.size());

            /*
             * 优先对 Aliyun, baidu, doubao 三个服务源的模型按模型名排序，并加入结果
             */
            prioritizedModels.stream().filter(chatServices::containsKey).forEach(serviceName -> {
                chatServices.get(serviceName)
                    .keySet()
                    .stream()
                    .filter(modelId -> allowedChatServices.stream()
                        .anyMatch(cs -> cs.equals(chatServices.get(serviceName).get(modelId))))
                    .forEach(modelId -> {
                        result.add(serviceName + "上的" + modelId);
                    });
            });

            // List<UserModelAccessConfig.ModelAccess> allowedMA = userModelAccessService
            // .getUserAllowedModelAccess(userUUID);
            // 对其余服务源的模型按模型名排序，并加入结果
            chatServices.keySet()
                .stream()
                .filter(serviceName -> !prioritizedModels.contains(serviceName))
                .forEach(serviceName -> {
                    chatServices.get(serviceName)
                        .keySet()
                        .stream()
                        .filter(modelId -> allowedChatServices.stream()
                            .anyMatch(cs -> cs.equals(chatServices.get(serviceName).get(modelId))))
                        .sorted()
                        .forEach(modelId -> result.add(serviceName + "上的" + modelId));
                });

            return Result.success(result);
        }
        catch (AccessDeniedException e) {
            return Result.error(List.of(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Failed to get models", e);
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
