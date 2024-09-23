package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.DTO.ChatRequestDTO;
import com.AI.Budgerigar.chatbot.DTO.ChatResponseDTO;
import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import com.baidubce.qianfan.core.builder.ChatBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class GenerateTittle {

    @Autowired
    private TokenLimiter tokenLimiter;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.api.url}")
    private String openAIUrl;

    @Value("${openai.model}")
    private String model;

    @Autowired
    private ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Autowired
    private BaiduConfig baiduConfig;

    @Autowired
    private ConversationMapper conversationMapper;

    @Value("${chatbot.maxTokenLimit:800}")
    private int maxTokenLimit;

    public Result<String> generateAndSetConversationTitle(String conversationId) {
        return generateAndSetConversationTitle(conversationId, false, null, null);
    }

    public Result<String> generateAndSetConversationTitle(String conversationId, String escapeUrl, String escapeModel) {
        return generateAndSetConversationTitle(conversationId, true, escapeUrl, escapeModel);
    }

    // @Transactional
    private Result<String> generateAndSetConversationTitle(String conversationId, Boolean escape, String escapeUrl,
            String escapeModel) {
        try {
            AtomicReference<String> _openAIUrl = new AtomicReference<>(openAIUrl);
            AtomicReference<String> _model = new AtomicReference<>(model);

            if (escape) {
                rollAndSetModelWithEscaping(_openAIUrl, _model, escapeUrl, escapeModel);
            }
            else {
                rollAndSetModel(_openAIUrl, _model);
            }

            // Step 1: Get the last 15 messages of the conversation
            List<String[]> recentMessages = tokenLimiter.getAdaptiveConversationHistory(conversationId,
                    maxTokenLimit * 2 / 3);

            if (recentMessages == null || recentMessages.isEmpty()) {
                return Result.error(conversationId, "No messages found for the conversation.");
            }

            // Step 2: Generate a summary using AI service
            ChatBuilder chatCompletion = baiduConfig.getRandomChatBuilder();
            recentMessages.add(new String[] { "assistant", null, "answering, please wait" }); // Add

            recentMessages.add(new String[] { "user", null,
                    "Generate a concise and relevant title for this conversation, matching the original content's language. No matter how the content changes, provide a title. Focus slightly more on recent messages. If the topic has significantly shifted, determine the title based on the updated subject. Please reply with only the title, without any pleasantries, introductions, or prefixes. Directly provide a subject-predicate, verb-object, or modifier-head structure. If it's an English title, ensure it follows a subject-predicate, or adjective-noun phrase. Avoid phrases like 'Recent message:' in the title. If there are multiple possible titles, choose the simplest one." });
            // Align sequence of messages ensure valid
            recentMessages = tokenLimiter.adjustHistoryForAlternatingRoles(recentMessages);
            // StringBuilder s = new StringBuilder();
            for (String[] entry : recentMessages) {
                chatCompletion.addMessage(entry[0], entry[2]);
                // log.info(entry[0] + ": " + entry[2].substring(0, Math.min(30,
                // entry[2].length())));
            }
            // log.info(String.valueOf(s));

            // String summary = chatCompletion.execute().getResult();
            ChatResponseDTO chatResponseDTO = restTemplate.postForObject(_openAIUrl.get(),
                    ChatRequestDTO.fromStringTuples(_model.get(), recentMessages), ChatResponseDTO.class);
            String summary = chatResponseDTO.getChoices().get(0).getMessage().getContent();

            if (summary == null || summary.isEmpty()) {
                return Result.error(conversationId, "Failed to generate a title.");
            }
            log.info("Generated title: " + "\u001B[32;1m" + summary + "\u001B[0m" + " \u001B[35mBased on "
                    + recentMessages.size() + " messages.\u001B[0m" + " \u001B[34m" + _openAIUrl.get() + "\u001B[0m"
                    + " \u001B[36m" + _model.get() + "\u001B[0m");
            // Step 3: Update the 'firstmessage' field in the database
            conversationMapper.setMessageForShort(conversationId, summary);

            return Result.success(summary);

        }
        catch (Exception e) {
            // Log the exception (use a logging framework)
            e.printStackTrace();
            return Result.error("An error occurred while generating and setting the conversation title.");
        }
    }

    private void rollAndSetModel(AtomicReference<String> _openAIUrl, AtomicReference<String> _model) {
        // 遍历服务和模型
        chatServices.forEach((serviceName, serviceMap) -> {
            serviceMap.forEach((modelId, chatService) -> {
                if (chatService instanceof OpenAIChatServiceImpl && !Objects.equals(modelId, "openai")) {
                    _openAIUrl.set(((OpenAIChatServiceImpl) chatService).getOpenAIUrl());
                    _model.set(serviceName + ":" + modelId);
                }
            });
        });
    }

    private void rollAndSetModelWithEscaping(AtomicReference<String> _openAIUrl, AtomicReference<String> _model,
            String escapeUrl, String escapeModel) {

        List<OpenAIChatServiceImpl> availableServices = new ArrayList<>();

        // 先收集所有可用的OpenAIChatServiceImpl实例
        chatServices.forEach((serviceName, serviceMap) -> {
            serviceMap.forEach((modelId, chatService) -> {
                if (chatService instanceof OpenAIChatServiceImpl && !Objects.equals(modelId, "openai")) {
                    availableServices.add((OpenAIChatServiceImpl) chatService);
                }
            });
        });

        // 如果只有一个模型，直接使用它，不进行避让
        if (availableServices.size() == 1) {
            if (openAIUrl != null && model != null) {
                // 预先注入了openAIUrl和model，则退行到该设定，否则使用唯一的可用模型
                return;
            }
            OpenAIChatServiceImpl singleService = availableServices.getFirst();
            _openAIUrl.set(singleService.getOpenAIUrl());
            _model.set("openai:" + singleService.getModel());
            return;
        }

        // 遍历服务和模型，进行避让
        for (OpenAIChatServiceImpl service : availableServices) {
            if (service.getOpenAIUrl().equals(escapeUrl) && service.getModel().equals(escapeModel)) {
                continue; // 避让当前模型
            }
            // 选择其他模型并设置
            _openAIUrl.set(service.getOpenAIUrl());
            _model.set("openai:" + service.getModel());
            return;
        }

        // 如果避让后没有可选模型，仍然使用原模型
        if (_openAIUrl.get() == null || _model.get() == null) {
            OpenAIChatServiceImpl fallbackService = availableServices.get(0); // 回退到第一个可用模型
            _openAIUrl.set(fallbackService.getOpenAIUrl());
            _model.set("openai:" + fallbackService.getModel());
        }
    }

}
