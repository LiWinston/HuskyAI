package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.ChatRequestDTO;
import com.AI.Budgerigar.chatbot.DTO.ChatResponseDTO;
import com.AI.Budgerigar.chatbot.Entity.Conversation;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.StreamChatService;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@NoArgsConstructor
public class OpenAIChatServiceImpl implements ChatService, StreamChatService {

    @Autowired
    DateTimeFormatter dateTimeFormatter;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    @Setter
    private RestTemplate restTemplate;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Autowired
    private ChatMessagesMongoDAO chatMessagesMongoDAO;

    @Autowired
    private ChatSyncServiceImpl chatSyncService;

    @Autowired
    private TokenLimiter tokenLimiter;

    @Getter
    @Setter
    @Value("${openai.model:}")
    private String model;

    @Getter
    @Setter
    @Value("${openai.api.url:}")
    private String openAIUrl;

    @Getter
    @Setter
    @Value("${openai.api.key:}")
    private String openaiApiKey;

    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private preChatBehaviour preChatBehaviour;

    @Autowired
    private ConversationMapper conversationMapper;

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @Override
    public Result<String> chat(String prompt, String conversationId) {
        log.info("chat with \u001B[34m{}\u001B[0m using model \u001B[32m{}\u001B[0m FROM {}", openAIUrl, model,
                OpenAIChatServiceImpl.class.getName());
        try {
            List<String[]> conversationHistory = preChatBehaviour.getHistoryPreChat(this, prompt, conversationId);

            // Create ChatRequestDTO from a String[] list using the factory method.
            ChatRequestDTO chatRequestDTO = ChatRequestDTO.fromStringTuples(model, conversationHistory);

            ChatResponseDTO chatResponseDTO = restTemplate.postForObject(openAIUrl, chatRequestDTO,
                    ChatResponseDTO.class);

            if (chatResponseDTO == null || chatResponseDTO.getChoices() == null
                    || chatResponseDTO.getChoices().isEmpty()) {
                throw new Exception("No response from API");
            }

            String result = chatResponseDTO.getChoices().get(0).getMessage().getContent();

            log.debug("Response from \u001B[34m{}\u001B[0m: \u001B[32m{}\u001B[0m", chatResponseDTO.getModel(),
                    result.substring(0, Math.min(40, result.length())));

            updateConversationHistory(conversationId, result);

            return Result.success(result,
                    "From " + chatResponseDTO.getModel() + ", Referenced " + conversationHistory.size() + " messages.");
        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", OpenAIChatServiceImpl.class.getName(), e.getMessage());
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    "Query failed. Please try again.");
            throw new RuntimeException("Error processing chat request", e);
        }
    }

    @PostConstruct
    public void init() {
        // Set up a connection pool and keep the connection alive.
        ConnectionProvider provider = ConnectionProvider.builder("custom")
            .maxConnections(500) // Set the largest number of connections
            .pendingAcquireTimeout(Duration.ofSeconds(60)) // Waiting time for connection
            .maxIdleTime(Duration.ofSeconds(30)) // set the maximum idle time
            .maxLifeTime(Duration.ofMinutes(5)) // set the maximum life time
            .build();

        this.webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider).compress(true).keepAlive(true)))
            .baseUrl(openAIUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // set
                                                                                       // the
                                                                                       // content
                                                                                       // type
            .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache") // set the cache control
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey) // set API
                                                                                // key
            .build();
    }

    public Flux<Result<String>> chatFlux(String prompt, String conversationId) {
        log.info("Flux chat with \u001B[34m{}\u001B[0m using model \u001B[32m{}\u001B[0m FROM {}", openAIUrl, model,
                OpenAIChatServiceImpl.class.getName());
        List<String[]> conversationHistory = preChatBehaviour.getHistoryPreChat(this, prompt, conversationId);
        ChatRequestDTO requestDTO = ChatRequestDTO.fromStringTuples(model, conversationHistory);
        requestDTO.setStream(true);

        StringBuilder contentBuilder = new StringBuilder();
        return webClient.post()
            .headers(httpHeaders -> httpHeaders.set("Content-Type", "application/json"))
            .bodyValue(requestDTO)
            .retrieve()
            .bodyToFlux(String.class)
            .flatMap(this::parseJsonChunk)
            .map(chatResponseDTO -> {
                String content = extractContentFromFirstChoice(chatResponseDTO);
                log.debug("Response from \u001B[34m{}\u001B[0m: \u001B[32m{}\u001B[0m", chatResponseDTO.getModel(),
                        content.substring(0, Math.min(40, content.length())));
                contentBuilder.append(content);
                var finishReason = chatResponseDTO.getChoices().get(0).getFinish_reason();
                if (finishReason != null && !finishReason.isEmpty()) {
                    updateConversationHistory(conversationId, contentBuilder.toString());
                    log.info("Conversation finished: " + finishReason + ": \u001B[32m"
                            + contentBuilder.substring(0, Math.min(30, contentBuilder.length())) + "\u001B[0m");
                    return Result.success(content, "From " + chatResponseDTO.getModel() + ", Referenced "
                            + conversationHistory.size() + " messages.");
                }
                return Result.success(content);
            })
            .onErrorResume(e -> {
                log.error("Error occurred in {}: {}", OpenAIChatServiceImpl.class.getName(), e.getMessage());
                chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                        "Query failed due to " + e.getMessage());
                return Flux.error(new RuntimeException("Error processing chat request" + e.getLocalizedMessage(), e));
            });
    }

    private Flux<ChatResponseDTO> parseJsonChunk(String chunk) {
        try {
            String jsonStr = chunk.startsWith("data: ") ? chunk.substring(6) : chunk;
            // log.info("jsonStr: {}", jsonStr);

            if ("[DONE]".equals(jsonStr.trim())) {
                // log.info("DONEDONE");
                return Flux.empty();
            }

            ChatResponseDTO responseDTO = objectMapper.readValue(jsonStr, ChatResponseDTO.class);
            // log.info("responseDTO: {}", responseDTO);
            return Flux.just(responseDTO);
        }
        catch (JsonProcessingException e) {
            log.error("Error parsing JSON chunk: {}", chunk, e);
            return Flux.empty();
        }
    }

    private String extractContentFromFirstChoice(ChatResponseDTO chatResponseDTO) {
        if (chatResponseDTO.getChoices() != null && !chatResponseDTO.getChoices().isEmpty()) {
            ChatResponseDTO.Choice choice = chatResponseDTO.getChoices().get(0);
            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                return choice.getDelta().getContent();
            }
        }
        return "";
    }

    private void updateConversationHistory(String conversationId, String response) {
        chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(), response);

        int redisLength = chatMessagesRedisDAO.getConversationHistory(conversationId).size();
        int mongoLength = chatMessagesMongoDAO.getConversationLengthById(conversationId);
        int diff = redisLength - mongoLength;
        log.info("Redis length: {}, MongoDB length: {}, diff: {} FROM {}", redisLength, mongoLength, diff,
                OpenAIChatServiceImpl.class.getName());
        if (Math.abs(diff) > 5) {
            executorService.submit(() -> chatSyncService.updateHistoryFromRedis(conversationId));
        }

        // Update the last conversation time in SQL.
        executorService.submit(() -> {
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation != null) {
                conversation.setLastMessageAt(LocalDateTime.from(Instant.now()));
                // 更新对话时间时清除缓存
                String uuid = conversation.getUuid();
                if (uuid != null) {
                    clearConversationCache(uuid);
                }
                conversationMapper.updateById(conversation);
            }
            else {
                log.warn("Conversation with id {} not found, unable to update lastMessageAt", conversationId);
            }
        });
    }

    @CacheEvict(value = {"conversations", "conversationsPage"}, key = "#uuid")
    public void clearConversationCache(String uuid) {
        // 方法体可以为空，注解会处理缓存清除
    }

    // private int getMongoConversationLength(String conversationId) {
    // // Logic to obtain session length from MongoDB.
    // // Assume that ChatMessagesMongoDAO provides a method to get the conversation
    // length.
    // return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    // }

    @Override
    public String getCallingClassName() {
        return OpenAIChatServiceImpl.class.getName();
    }

    @Override
    public void logInfo(String message) {
        ChatService.super.logInfo(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        ChatService.super.logError(message, throwable);
    }

}
