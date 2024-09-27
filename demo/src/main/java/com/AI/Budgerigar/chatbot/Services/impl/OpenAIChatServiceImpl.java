package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.DTO.ChatRequestDTO;
import com.AI.Budgerigar.chatbot.DTO.ChatResponseDTO;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.StreamChatService;
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

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @Override
    public Result<String> chat(String prompt, String conversationId) {
        log.info("Flux chat with \u001B[34m{}\u001B[0m using model \u001B[32m{}\u001B[0m FROM {}", openAIUrl, model,
                OpenAIChatServiceImpl.class.getName());
        try {
            List<String[]> conversationHistory = preChatBehaviour.getHistoryPreChat(this, prompt, conversationId);

            // 使用工厂方法从 String[] 列表创建 ChatRequestDTO
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
        // 设置连接池，保活连接
        ConnectionProvider provider = ConnectionProvider.builder("custom")
            .maxConnections(500) // 设置最大连接数
            .pendingAcquireTimeout(Duration.ofSeconds(60)) // 等待连接池的超时时间
            .maxIdleTime(Duration.ofSeconds(30)) // 空闲连接保活时间
            .maxLifeTime(Duration.ofMinutes(5)) // 连接最长保留时间
            .build();

        this.webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider).compress(true).keepAlive(true)))
            .baseUrl(openAIUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // 设置内容类型
            .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache") // 设置缓存控制
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey) // 设置 API
                                                                                // Key
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
    }

    // private int getMongoConversationLength(String conversationId) {
    // // 从 MongoDB 获取会话长度的逻辑
    // // 假设 ChatMessagesMongoDAO 提供了获取会话长度的方法
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
