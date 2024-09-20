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
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@NoArgsConstructor
public class OpenAIChatServiceImpl implements ChatService, StreamChatService {

    @Autowired
    private ExecutorService executorService;

    @Autowired
    DateTimeFormatter dateTimeFormatter;

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

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    List<String[]> getHistoryPreChat(String prompt, String conversationId) {
        chatSyncService.updateRedisFromMongo(conversationId);

        chatMessagesRedisDAO.maintainMessageHistory(conversationId);

        // 添加用户输入到 Redis 对话历史
        chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(),
                StringEscapeUtils.escapeHtml4(prompt));

        // 从 Redis 中获取对话历史
        List<String[]> conversationHistory;
        try {
            conversationHistory = tokenLimiter.getAdaptiveConversationHistory(conversationId, 16000);
            log.info("自适应缩放到" + conversationHistory.size() + "条消息");
            // for (String[] entry : conversationHistory) {
            // log.info("{} : {}", entry[0], entry[2].substring(0, Math.min(20,
            // entry[2].length())));
            // }
            return conversationHistory;
        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", TokenLimiter.class.getName(), e.getMessage(), e);
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    "Query failed. Please try again.");
            throw new RuntimeException("Error processing chat request", e);
        }
    }

    @Override
    public Result<String> chat(String prompt, String conversationId) {
        try {
            List<String[]> conversationHistory = getHistoryPreChat(prompt, conversationId);

            // 使用工厂方法从 String[] 列表创建 ChatRequestDTO
            ChatRequestDTO chatRequestDTO = ChatRequestDTO.fromStringTuples(model, conversationHistory);

            ChatResponseDTO chatResponseDTO = restTemplate.postForObject(openAIUrl, chatRequestDTO,
                    ChatResponseDTO.class);

            if (chatResponseDTO == null || chatResponseDTO.getChoices() == null
                    || chatResponseDTO.getChoices().isEmpty()) {
                throw new Exception("No response from API");
            }

            String result = chatResponseDTO.getChoices().get(0).getMessage().getContent();

            log.info("Response from \u001B[34m{}\u001B[0m: \u001B[32m{}\u001B[0m", chatResponseDTO.getModel(),
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

    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
            .baseUrl(openAIUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // 设置内容类型
            .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache") // 设置缓存控制
            // .defaultHeader(HttpHeaders.CONNECTION, "keep-alive") // 设置连接保持
            .build();
    }

    public Flux<Result<String>> chatFlux(String prompt, String conversationId) {
        List<String[]> conversationHistory = getHistoryPreChat(prompt, conversationId);
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
                contentBuilder.append(content);
                var finishReason = chatResponseDTO.getChoices().get(0).getFinish_reason();
                if (finishReason != null && !finishReason.isEmpty()) {
                    updateConversationHistory(conversationId, contentBuilder.toString());
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
