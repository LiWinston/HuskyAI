package com.AI.Budgerigar.chatbot.API.Svc;

import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.DTO.ChatRequestDTO;
import com.AI.Budgerigar.chatbot.DTO.ChatResponseDTO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.chatServicesManageService;
import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import com.AI.Budgerigar.chatbot.result.Result;
import com.baidubce.qianfan.core.builder.ChatBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class SummarizeSvc {

    @Autowired
    private BaiduConfig baiduConfig;

    @Autowired
    private chatServicesManageService chatServicesManageService;

    public Result<String> summarizeEmail(String text) {
        String summary;
        try {
            AtomicReference<String> openAIUrl = new AtomicReference<>();
            AtomicReference<String> model = new AtomicReference<>();
            AtomicReference<String> apikey = new AtomicReference<>();

            rollAndSetModel(openAIUrl, model, apikey);

            if (openAIUrl.get() == null || model.get() == null || apikey.get() == null) {
                log.info("No available OpenAI model found.");
            }
            else {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getInterceptors().add((request, body, execution) -> {
                    request.getHeaders().add("Authorization", "Bearer " + apikey.get());
                    return execution.execute(request, body);
                });

                String prompt = "Please summarize the following text into a suitable Eamil Subject. A good subject should be concise and informative. Dont be just repeating the content. \n\n\r"
                        + text;
                ChatRequestDTO chatRequestDTO = new ChatRequestDTO(model.get(), prompt, null);
                ChatResponseDTO chatResponseDTO = restTemplate.postForObject(openAIUrl.get(), chatRequestDTO,
                        ChatResponseDTO.class);
                summary = chatResponseDTO.getChoices().get(0).getMessage().getContent();

                if (summary != null && !summary.isEmpty()) {
                    return Result.success(summary);
                }
            }

            summary = generateSummaryWithBaidu(text);
            if (summary == null || summary.isEmpty()) {
                return Result.error("Failed to generate a summary.");
            }

            return Result.success(summary);

        }
        catch (Exception e) {
            log.error("Error generating summary: ", e);
            return Result.error("An error occurred while generating the summary.");
        }
    }

    private void rollAndSetModel(AtomicReference<String> openAIUrl, AtomicReference<String> model,
            AtomicReference<String> apikey) {
        ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> chatServices = chatServicesManageService
            .getChatServices();

        // 使用随机取值的方式，从双层HashMap中选取任意一个服务
        List<Map.Entry<String, ConcurrentHashMap<String, ChatService>>> outerEntries = new ArrayList<>(
                chatServices.entrySet());
        if (outerEntries.isEmpty()) {
            return; // 若无可用服务，直接返回
        }

        // 随机选择一个服务
        Map.Entry<String, ConcurrentHashMap<String, ChatService>> randomOuterEntry = outerEntries
            .get(ThreadLocalRandom.current().nextInt(outerEntries.size()));
        ConcurrentHashMap<String, ChatService> serviceMap = randomOuterEntry.getValue();

        List<Map.Entry<String, ChatService>> innerEntries = new ArrayList<>(serviceMap.entrySet());
        if (innerEntries.isEmpty()) {
            return; // 若内部服务为空，直接返回
        }

        // 从内部Map中随机取一个
        Map.Entry<String, ChatService> randomInnerEntry = innerEntries
            .get(ThreadLocalRandom.current().nextInt(innerEntries.size()));
        ChatService chatService = randomInnerEntry.getValue();

        if (chatService instanceof OpenAIChatServiceImpl oachatService) {
            openAIUrl.set(oachatService.getOpenAIUrl());
            model.set(randomInnerEntry.getKey());
            apikey.set(oachatService.getOpenaiApiKey());
        }
    }

    private String generateSummaryWithBaidu(String text) {
        try {
            ChatBuilder chatCompletion = baiduConfig.getRandomChatBuilder();
            chatCompletion.addMessage("user", text);
            return chatCompletion.execute().getResult();
        }
        catch (Exception e) {
            log.error("Error using Baidu service: ", e);
            return null;
        }
    }

}