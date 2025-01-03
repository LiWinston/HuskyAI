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

    @Value("${openai.api.key}")
    private String apikey;

    @Autowired
    private chatServicesManageService chatServicesManageService;

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
            AtomicReference<String> _apikey = new AtomicReference<>(apikey);

            if (escape) {
                rollAndSetModelWithEscaping(_openAIUrl, _model, _apikey, escapeUrl, escapeModel);
            }
            else {
                rollAndSetModel(_openAIUrl, _model, _apikey);
            }

            // Step 1: Get the last 15 messages of the conversation
            List<String[]> recentMessages = tokenLimiter.getAdaptiveConversationHistory(conversationId,
                    maxTokenLimit * 2 / 3);

            if (recentMessages == null || recentMessages.isEmpty()) {
                return Result.error(conversationId, "No messages found for the conversation.");
            }

            // ChatBuilder chatCompletion = baiduConfig.getRandomChatBuilder();
            recentMessages.add(new String[] { "assistant", null, "answering, please wait" }); // Add
            //
            recentMessages.add(new String[] { "user", null,
                    "Generate a concise and relevant title for this conversation, matching the original content's language. No matter how the content changes, provide a title. Focus slightly more on recent messages. If the topic has significantly shifted, determine the title based on the updated subject. Please reply with only the title, without any pleasantries, introductions, or prefixes. Directly provide a subject-predicate, verb-object, or modifier-head structure. If it's an English title, ensure it follows a subject-predicate, or adjective-noun phrase. Avoid phrases like 'Recent message:' in the title. If there are multiple possible titles, choose the simplest one." });
            // // Align sequence of messages ensure valid
            // recentMessages =
            tokenLimiter.adjustHistoryForAlternatingRoles(recentMessages);
            // // StringBuilder s = new StringBuilder();
            // for (String[] entry : recentMessages) {
            // chatCompletion.addMessage(entry[0], entry[2]);
            // // log.info(entry[0] + ": " + entry[2].substring(0, Math.min(30,
            // // entry[2].length())));
            // }
            // log.info(String.valueOf(s));

            // String summary = chatCompletion.execute().getResult();

            var restTemplate = new RestTemplate() {
                {
                    getInterceptors().add((request, body, execution) -> {
                        request.getHeaders().add("Authorization", "Bearer " + _apikey.get());
                        return execution.execute(request, body);
                    });
                }
            };

            ChatRequestDTO chatRequestDTO = ChatRequestDTO.fromStringTuples(_model.get(), recentMessages);
//            log.info("ChatRequestDTO: " + chatRequestDTO.toString());
            ChatResponseDTO chatResponseDTO = restTemplate.postForObject(_openAIUrl.get(), chatRequestDTO,
                    ChatResponseDTO.class);
            String summary = chatResponseDTO.getChoices().get(0).getMessage().getContent();

            if (summary == null || summary.isEmpty()) {
                summary = generateTitleWithBaidu(recentMessages);
                if (summary == null || summary.isEmpty()) {
                    return Result.error(conversationId, "Failed to generate a title.");
                }
                log.info("Generated title: " + "\u001B[32;1m" + summary + "\u001B[0m" + " \u001B[35mBased on "
                        + recentMessages.size() + " messages.\u001B[0m" + " \u001B[34m" + "baidu" + "\u001B[0m"
                        + " \u001B[36m" + baiduConfig.getCurrentModel() + "\u001B[0m");
            }
            else {
                log.info("Generated title: " + "\u001B[32;1m" + summary + "\u001B[0m" + " \u001B[35mBased on "
                        + recentMessages.size() + " messages.\u001B[0m" + " \u001B[34m" + _openAIUrl.get() + "\u001B[0m"
                        + " \u001B[36m" + _model.get() + "\u001B[0m");
            }
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

    private void rollAndSetModel(AtomicReference<String> _openAIUrl, AtomicReference<String> _model,
            AtomicReference<String> _apikey) {
        var chatServices = chatServicesManageService.getChatServices();
        // Traverse services and models.
        chatServices.forEach((serviceName, serviceMap) -> {
            serviceMap.forEach((modelId, chatService) -> {
                if (chatService instanceof OpenAIChatServiceImpl oachatService && !Objects.equals(modelId, "openai")) {
                    _openAIUrl.set(oachatService.getOpenAIUrl());
                    _model.set(modelId);
                    _apikey.set(oachatService.getOpenaiApiKey());
                }
            });
        });
    }

    private void rollAndSetModelWithEscaping(AtomicReference<String> _openAIUrl, AtomicReference<String> _model,
            AtomicReference<String> _apikey, String escapeUrl, String escapeModel) {
        var chatServices = chatServicesManageService.getChatServices();

        List<OpenAIChatServiceImpl> availableServices = new ArrayList<>();

        log.debug("escapeUrl: {}, escapeModel: {}", escapeUrl, escapeModel);
        // First collect all available OpenAIChatServiceImpl instances.
        chatServices.forEach((serviceName, serviceMap) -> {
            serviceMap.forEach((modelId, chatService) -> {
                if (chatService instanceof OpenAIChatServiceImpl && !Objects.equals(modelId, "openai")) {
                    // Instances of OpenAIChatServiceImpl that are not openai are added to
                    // the availableServices list.
                    availableServices.add((OpenAIChatServiceImpl) chatService);
                }
            });
        });

        log.debug("availableServices: {}", availableServices);

        // If there is only one non-OpenAI OpenAIChatServiceImpl instance, use that
        // instance directly.
        if (availableServices.size() == 1) {
            // if (openAIUrl != null && model != null) {
            // // If openAIUrl and model are pre-injected, revert to that setting and use
            // the only non-openai model.
            // return;
            // }
            log.debug("There is only one non-openai OpenAIChatServiceImpl instance, use that instance directly.");
            OpenAIChatServiceImpl singleService = availableServices.getFirst();
            _openAIUrl.set(singleService.getOpenAIUrl());
            _model.set(singleService.getModel());
            _apikey.set(singleService.getOpenaiApiKey());
            return;
        }

        // Traverse services and models to avoid.
        for (OpenAIChatServiceImpl service : availableServices) {
            if (service.getOpenAIUrl().equals(escapeUrl) && service.getModel().equals(escapeModel)) {
                continue; // Avoid the current model.
            }
            log.debug("Avoid successfully {}", service.getModel());
            // Select another model and set it.
            _openAIUrl.set(service.getOpenAIUrl());
            _model.set(service.getModel());
            _apikey.set(service.getOpenaiApiKey());
            return;
        }

        // If there is no alternative model after avoiding, still use the original model.
        if (_openAIUrl.get() == null || _model.get() == null) {
            log.debug("After yielding, there is no alternative model, still using the original model.");
            OpenAIChatServiceImpl fallbackService = availableServices.get(0); // Revert to
                                                                              // the first
                                                                              // available
                                                                              // model.
            _openAIUrl.set(fallbackService.getOpenAIUrl());
            _model.set(fallbackService.getModel());
            _apikey.set(fallbackService.getOpenaiApiKey());
        }
    }

    private String generateTitleWithBaidu(List<String[]> recentMessages) {
        try {
            ChatBuilder chatCompletion = baiduConfig.getRandomChatBuilder();
            recentMessages.add(new String[] { "assistant", null, "Still to be answered" });
            recentMessages.add(new String[] { "user", null,
                    "Generate a concise and relevant title for this conversation, matching the original content's language. No matter how the content changes, provide a title. Focus slightly more on recent messages. If the topic has significantly shifted, determine the title based on the updated subject. Please reply with only the title, without any pleasantries, introductions, or prefixes. Directly provide a subject-predicate, verb-object, or modifier-head structure. If it's an English title, ensure it follows a subject-predicate, or adjective-noun phrase. Avoid phrases like 'Recent message:' in the title. If there are multiple possible titles, choose the simplest one." });

            recentMessages = tokenLimiter.adjustHistoryForAlternatingRoles(recentMessages);

            for (String[] entry : recentMessages) {
                chatCompletion.addMessage(entry[0], entry[2]);
            }

            return chatCompletion.execute().getResult();
        }
        catch (Exception e) {
            log.error("Error using Baidu service: ", e);
            return null;
        }
    }

}
