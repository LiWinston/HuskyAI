package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.DTO.ChatRequestDTO;
import com.AI.Budgerigar.chatbot.DTO.ChatResponseDTO;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import com.baidubce.qianfan.core.builder.ChatBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class GenerateTittle {

    @Autowired
    private TokenLimiter tokenLimiter;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.api.url:${PC.LMStudioServer.url}}")
    private String openAIUrl;

    @Value("${openai.model:${PC.LMStudioServer.model}}")
    private String model;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Autowired
    private BaiduConfig baiduConfig;

    @Autowired
    private ConversationMapper conversationMapper;

    // @Transactional
    public Result<String> generateAndSetConversationTitle(String conversationId) {
        try {
            // Step 1: Get the last 15 messages of the conversation
            List<String[]> recentMessages = tokenLimiter.getFixedHistory(conversationId,
                    (int) Math.min(15, chatMessagesRedisDAO.getMessageCount(conversationId)));

            if (recentMessages == null || recentMessages.isEmpty()) {
                return Result.error(conversationId, "No messages found for the conversation.");
            }

            // Step 2: Generate a summary using AI service
            ChatBuilder chatCompletion = baiduConfig.getRandomChatBuilder();
            recentMessages.add(new String[] { "assistant", null, "Still to be answered" }); // Add

            recentMessages.add(new String[] { "user", null,
                    "Generate a concise and relevant title for this conversation, matching the original content's language. No matter how the content changes, provide a title. Focus slightly more on recent messages. If the topic has significantly shifted, determine the title based on the updated subject. Please reply with only the title, without any pleasantries, introductions, or prefixes. Directly provide a subject-predicate, verb-object, or modifier-head structure. If it's an English title, ensure it follows a subject-predicate, or adjective-noun phrase. Avoid phrases like 'Recent message:' in the title. If there are multiple possible titles, choose the simplest one." });
            // Align sequence of messages ensure valid
            recentMessages = tokenLimiter.adjustHistoryForAlternatingRoles(recentMessages);
            // StringBuilder s = new StringBuilder();
            for (String[] entry : recentMessages) {
                chatCompletion.addMessage(entry[0], entry[2]);
                log.info(entry[0] + ": " + entry[2].substring(0, Math.min(30, entry[2].length())));
            }
            // log.info(String.valueOf(s));

            // String summary = chatCompletion.execute().getResult();
            ChatResponseDTO chatResponseDTO = restTemplate.postForObject(openAIUrl,
                    ChatRequestDTO.fromStringTuples(model, recentMessages), ChatResponseDTO.class);
            String summary = chatResponseDTO.getChoices().get(0).getMessage().getContent();

            if (summary == null || summary.isEmpty()) {
                return Result.error(conversationId, "Failed to generate a title.");
            }
            log.info("Generated title: " + summary);

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

}
