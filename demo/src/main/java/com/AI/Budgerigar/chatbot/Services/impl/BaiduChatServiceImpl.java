package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.Services.StreamChatService;
import com.AI.Budgerigar.chatbot.result.Result;
import com.baidubce.qianfan.core.StreamIterator;
import com.baidubce.qianfan.core.builder.ChatBuilder;
import com.baidubce.qianfan.model.chat.ChatResponse;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

@Service
@Setter
@Slf4j
public class BaiduChatServiceImpl implements ChatService, StreamChatService {

    private static final Logger logger = Logger.getLogger(BaiduChatServiceImpl.class.getName());

    @Getter
    // public String conversationId;
    @Autowired
    DateTimeFormatter dateTimeFormatter;

    @Autowired
    ChatService.TokenLimitType tokenLimitType;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private TokenLimiter tokenLimiter;

    @Autowired
    private BaiduConfig baiduConfig;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    // // 使用 ThreadLocal 来存储 conversationId
    // private static final ThreadLocal<String> conversationIdThreadLocal =
    // ThreadLocal.withInitial(() -> "default_baidu_conversation");
    @Autowired
    private ChatMessagesMongoDAO chatMessagesMongoDAO;

    @Autowired
    private ChatSyncService chatSyncService;

    private final Gson gson = new Gson();

    private List<ChatBuilder> chatBuilders = new ArrayList<>();

    // public void setConversationId(String conversationId) {
    // this.conversationId = conversationId;
    // log.info("FE SET conversation ID to: " + conversationId);
    // }

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @Autowired
    private preChatBehaviour preChatBehaviour;

    @Override
    public Result<String> chat(String input, String conversationId) {
        try {
            // 从 Redis 中获取对话历史
            List<String[]> conversationHistory = preChatBehaviour.getHistoryPreChat(this, input, conversationId);

            // 创建 ChatCompletion 请求对象
            ChatBuilder chatCompletion = baiduConfig.getRandomChatBuilder();

            // 添加对话历史到请求对象中
            for (String[] entry : conversationHistory) {
                chatCompletion.addMessage(entry[0], entry[2]); // entry[0] 是角色，entry[2]
                                                               // 是内容
            }

            // 执行请求
            ChatResponse response = chatCompletion.execute();
            String result = response.getResult();
            logInfo(" # " + baiduConfig.getCurrentModel() + "\n" + result.substring(0, Math.min(20, result.length())));

            // 将助手的响应添加到 Redis 对话历史
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    // StringEscapeUtils.escapeHtml4(result));
                    result);
            // Calculate the difference in conversation length
            long redisLength = chatMessagesRedisDAO.getMessageCount(conversationId);
            int mongoLength = getMongoConversationLength(conversationId);
            long diff = redisLength - mongoLength;
            logger.info("Redis length: " + redisLength + ", MongoDB length: " + mongoLength + ", diff: " + diff);

            // If difference exceeds threshold, update MongoDB asynchronously
            if (Math.abs(diff) > 5) {
                executorService.submit(() -> {
                    chatSyncService.updateHistoryFromRedis(conversationId);
                });
            }

            return Result.success(result,
                    "Answer based on previous " + conversationHistory.size() + " messages Context~");
        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", BaiduChatServiceImpl.class.getName(), e.getMessage(), e);
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    "Query failed. Please try again.");
            throw new RuntimeException("Error processing chat request", e);
        }
    }

    @Override
    public void logInfo(String message) {
        ChatService.super.logInfo(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        ChatService.super.logError(message, throwable);
    }

    @Override
    public String getCallingClassName() {
        return ChatService.super.getCallingClassName();
    }

    @Override
    public Flux<Result<String>> chatFlux(String prompt, String conversationId) {
        List<String[]> conversationHistory = preChatBehaviour.getHistoryPreChat(this, prompt, conversationId);
        ChatBuilder chatBuilder = baiduConfig.getRandomChatBuilder();
        for (String[] entry : conversationHistory) {
            chatBuilder.addMessage(entry[0], entry[2]);
        }

        StringBuilder contentBuilder = new StringBuilder();

        return Flux.<Result<String>>create(emitter -> {
            Schedulers.boundedElastic().schedule(() -> {
                try {
                    StreamIterator<ChatResponse> iterator = chatBuilder.executeStream();
                    while (iterator.hasNext()) {
                        ChatResponse response = iterator.next();
                        String content = response.getResult();

                        log.info("Response from \u001B[34m{}\u001B[0m: \u001B[32m{}\u001B[0m",
                                baiduConfig.getCurrentModel(), content.substring(0, Math.min(40, content.length())));
                        contentBuilder.append(content);

                        emitter.next(Result.success(content));

                        if (response.getEnd()) {
                            updateConversationHistory(conversationId, contentBuilder.toString());
                            log.info("Conversation finished: \u001B[32m{}\u001B[0m",
                                    contentBuilder.substring(0, Math.min(30, contentBuilder.length())));
                            emitter.complete();
                        }
                    }
                }
                catch (Exception e) {
                    emitter.error(e);
                }
            });
        }).onErrorResume(e -> {
            log.error("Error occurred in {}: {}", BaiduChatServiceImpl.class.getName(), e.getMessage());
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                    "Query failed due to " + e.getMessage());
            return Flux.error(new RuntimeException("Error processing chat request: " + e.getLocalizedMessage(), e));
        });
    }

    private void updateConversationHistory(String conversationId, String response) {
        chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(), response);

        int redisLength = chatMessagesRedisDAO.getConversationHistory(conversationId).size();
        int mongoLength = chatMessagesMongoDAO.getConversationLengthById(conversationId);
        int diff = redisLength - mongoLength;
        log.info("Redis length: {}, MongoDB length: {}, diff: {} FROM {}", redisLength, mongoLength, diff,
                BaiduChatServiceImpl.class.getName());
        if (Math.abs(diff) > 5) {
            executorService.submit(() -> chatSyncService.updateHistoryFromRedis(conversationId));
        }
    }

    private int getMongoConversationLength(String conversationId) {
        // Implement the logic to get the conversation length from MongoDB
        // Assuming ChatMessagesMongoDAO has a method to get the conversation length
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }

}
