package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.Cache.CacheService;
import com.AI.Budgerigar.chatbot.DTO.PageDTO;
import com.AI.Budgerigar.chatbot.Entity.Conversation;
import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.Services.*;
import com.AI.Budgerigar.chatbot.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
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

import static com.AI.Budgerigar.chatbot.Constant.ApplicationConstant.MODELALIAS_MODELID_SEPARATOR;

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
    private ConversationService conversationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private com.AI.Budgerigar.chatbot.Services.chatServicesManageService chatServicesManageService;

    @Autowired
    private UserModelAccessService userModelAccessService;
    
    @Autowired
    private CacheService cacheService;

    // Get all conversation list from DB for user to choose.
    // Each conversation list contains conversation ID and conversation excerpt
    @GetMapping()
    public Result<?> getConversationList(@RequestParam String uuid) {
        try {
            // Use userService to check if the user exists.
            var userExists = userService.checkUserExistsByUuid(uuid);
            if (userExists.getCode() == 0) {
                return Result.error(userExists.getMsg());
            }

            // Obtain the user's conversation list and message excerpts.
            List<Conversation> conversations = userService.getConversations(uuid);
            return Result.success(conversations);

        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * Transmit ConversationId via GET to express the meaning of retrieving history
     * records.
     * @param uuid user id
     * @param conversationId conversation id
     * @return conversation history
     */
    @GetMapping("/{uuid}/{conversationId}")
    public Result<?> chat(@PathVariable String uuid, @PathVariable String conversationId) {
        if (!conversationService.checkConversationExists(uuid, conversationId)) {
            log.info("conversationId not exists, create new conversationId");
            try {
                Result<?> result = conversationService.createConversation(uuid, conversationId);
                if (result.getCode() == 0) {
                    return result;
                }
            } catch (Exception e) {
                return Result.error(e.getMessage());
            }
        }
        try {
            List<Message> messageList = chatSyncService.getHistory(conversationId);
            return Result.success(messageList);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<String> chatPost(@RequestBody Map<String, String> body) {
        var chatServices = chatServicesManageService.getChatServices();
        try {
            // 异步清除受影响的页面缓存
            cacheService.asyncClearAffectedConversationCaches()
                .exceptionally(throwable -> {
                    log.error("清除缓存失败", throwable);
                    return null;
                });
            
            String model = body.get("model");
            if (model == null) {
                model = "baidu";
            }
            String[] modelParts = model.split(MODELALIAS_MODELID_SEPARATOR);
            if (modelParts.length != 2) {
                return Result
                    .error("Invalid model format. Expected serviceName " + MODELALIAS_MODELID_SEPARATOR + " modelId");
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

            // Non-streaming chat
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
            // 异步清除受影响的页面缓存
            cacheService.asyncClearAffectedConversationCaches()
                .exceptionally(throwable -> {
                    log.error("清除缓存失败", throwable);
                    return null;
                });
            
            String model = body.get("model");
            if (model == null) {
                model = "baidu" + MODELALIAS_MODELID_SEPARATOR + "baidu";
            }

            String[] modelParts = model.split(MODELALIAS_MODELID_SEPARATOR);
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

            // Check if streaming is supported.
            if (chatService instanceof StreamChatService) {
                log.info("支持流式调用");
                return ((StreamChatService) chatService).chatFlux(body.get("prompt"), body.get("conversationId"))
                    .map(result -> {
                        // Convert the Result object to a JSON string and add a newline
                        // character.
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

    // Keep the first several models unchanged and sort the other models by the model names
    // within the service source.
    @PostMapping("/models")
    public Result<?> getModels(@RequestBody Map<String, String> body) {
        String userUUID = body.get("uuid");
        // // Check the health of the remote service.
        // chatServicesManageService.checkRemoteServicesHealth();

        ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices = chatServicesManageService
            .getChatServices();
        try {
            // Priority model list.
            List<String> prioritizedModels = Arrays.asList("PCLMStudio", "HuskyGPT", "Aliyun", "baidu", "doubao",
                    "MBP14LMStudio");

            List<ChatService> allowedChatServices = userModelAccessService.getUserAllowedChatServices(userUUID);
            log.info("Allowed chat services: " + allowedChatServices);

            // Filter out services that are currently unavailable on the server.
            List<String> result = new ArrayList<>(allowedChatServices.size());

            /*
             * Priority is given to sorting the models from the three service sources
             * Aliyun, Baidu, and Doubao by model name, and results are added.
             */
            prioritizedModels.stream().filter(chatServices::containsKey).forEach(serviceName -> {
                chatServices.get(serviceName)
                    .keySet()
                    .stream()
                    .filter(modelId -> allowedChatServices.stream()
                        .anyMatch(cs -> cs.equals(chatServices.get(serviceName).get(modelId))))
                    .forEach(modelId -> {
                        result.add(serviceName + MODELALIAS_MODELID_SEPARATOR + modelId);
                    });
            });

            // List<UserModelAccessConfig.ModelAccess> allowedMA = userModelAccessService
            // .getUserAllowedModelAccess(userUUID);
            // Sort the models of the remaining service sources by model name and include
            // the results.
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
                        .forEach(modelId -> result.add(serviceName + MODELALIAS_MODELID_SEPARATOR + modelId));
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
            // Use the deleteConversation method of userService and return the result.
            return chatSyncService.deleteConversation(uuid, conversationId);

        }
        catch (Exception e) {
            // Capture any exceptions and return an error response.
            return Result.error(e.getMessage());
        }
    }

    // 分页获取对话列表
    @GetMapping("/page")
    public Result<?> getConversationListWithPage(
            @RequestParam String uuid,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            log.info("分页获取对话列表: uuid={}, current={}, size={}", uuid, current, size);
            
            // 检查用户是否存在
            var userExists = userService.checkUserExistsByUuid(uuid);
            if (userExists.getCode() == 0) {
                log.error("用户不存在: {}", userExists.getMsg());
                return Result.error(userExists.getMsg());
            }

            // 创建分页参数
            PageDTO pageDTO = new PageDTO();
            pageDTO.setCurrent(current);
            pageDTO.setSize(size);
            
            // 获取分页后的对话列表
            PageInfo<Conversation> pageInfo = userService.getConversationsWithPage(uuid, pageDTO);
            
            log.info("成功获取分页对话列表: total={}, pages={}, current={}, size={}", 
                    pageInfo.getTotal(), 
                    pageInfo.getPages(), 
                    pageInfo.getPageNum(), 
                    pageInfo.getPageSize());
            
            return Result.success(pageInfo);
        } catch (Exception e) {
            log.error("获取分页对话列表异常", e);
            return Result.error("获取对话列表失败: " + e.getMessage());
        }
    }

}
