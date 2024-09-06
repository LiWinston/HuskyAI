package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import com.baidubce.qianfan.core.builder.ChatBuilder;
import com.baidubce.qianfan.model.chat.ChatResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

@Service
@Setter
@Slf4j
public class BaiduChatServiceImpl implements ChatService {

    private static final Logger logger = Logger.getLogger(BaiduChatServiceImpl.class.getName());

    @Getter
    // public String conversationId;
    @Autowired
    DateTimeFormatter dateTimeFormatter;

    @Autowired
    TokenLimitType tokenLimitType;

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

    @Autowired
    private ConversationMapper conversationMapper;

    private List<ChatBuilder> chatBuilders = new ArrayList<>();

    // public void setConversationId(String conversationId) {
    // this.conversationId = conversationId;
    // log.info("FE SET conversation ID to: " + conversationId);
    // }

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }

    @Override
    public Result<String> chat(String input, String conversationId) {
        try {
            chatSyncService.updateRedisFromMongo(conversationId);

            chatMessagesRedisDAO.maintainMessageHistory(conversationId);
            // 添加用户输入到 Redis 对话历史
            chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(),
                    StringEscapeUtils.escapeHtml4(input));

            // 创建 ChatCompletion 请求对象
            ChatBuilder chatCompletion = baiduConfig.getRandomChatBuilder();

            // 从 Redis 中获取对话历史
            List<String[]> conversationHistory = null;
            try {
                conversationHistory = tokenLimiter.getAdaptiveConversationHistory(conversationId, 1800);
                log.info("自适应缩放到" + conversationHistory.size() + "条消息");
                // for (String[] entry : conversationHistory) {
                // log.info("{} : {}", entry[0], entry[2].substring(0, Math.min(20,
                // entry[2].length())));
                // }
            }
            catch (Exception e) {
                log.error("Error occurred in {}: {}", TokenLimiter.class.getName(), e.getMessage(), e);
                chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(),
                        "Query failed. Please try again.");
                throw new RuntimeException("Error processing chat request", e);
            }

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
                    StringEscapeUtils.escapeHtml4(result));

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

    private int getMongoConversationLength(String conversationId) {
        // Implement the logic to get the conversation length from MongoDB
        // Assuming ChatMessagesMongoDAO has a method to get the conversation length
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }

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
                                                                                            // a
                                                                                            // dummy
                                                                                            // entry
                                                                                            // to
                                                                                            // ensure
                                                                                            // the
                                                                                            // AI
                                                                                            // model
                                                                                            // has
                                                                                            // enough
                                                                                            // context
            recentMessages.add(new String[] { "user", null,
                    "为此对话生成一个简洁且相关的标题，并匹配原始内容的语言，无论内容如何变化，都要提供标题。" + "稍微更侧重于最近的消息，如果主题发生过大变化，请根据更新后的主题来确定标题。"
                            + "请仅回复标题内容，不需要任何寒暄、引入和前缀词，直接给出主谓、动宾或偏正，如果是英文标题则主谓、定语中心语。"
                            + "更不要包含例如“最近消息：”这样的引入短语，若有多种可能的标题，请选择最简洁的一个。" });
            // This is for indicating the details used to generate the title, Now fully
            // tested so can be removed
            // 这是用于指示生成标题所使用的详细信息列表，现在已经完全测试，因此可以删除
            recentMessages = tokenLimiter.adjustHistoryForAlternatingRoles(recentMessages);
            // StringBuilder s = new StringBuilder();
            for (String[] entry : recentMessages) {
                chatCompletion.addMessage(entry[0], entry[2]);
                // s.append(entry[2]).append(" ");
            }
            // log.info(String.valueOf(s));

            String summary = chatCompletion.execute().getResult();

            if (summary == null || summary.isEmpty()) {
                return Result.error(conversationId, "Failed to generate a title.");
            }
            log.info("Generated title: " + summary);
//
//            // recentMessages.add(new String[] { "user", null, "Show me the concluded
//            // title of this conversation." });
//            chatCompletion.addAssistantMessage(summary);
//            chatCompletion.addUserMessage("Tittle can not contain any greeting, introduction, or prefix words, "
//                    + "just Show the core content. Of course, if you think there's no need to change the title, you can remain your original reply."
//                    + "Also dont add any dummy phrases before the title, just give the subject. ");
//            // recentMessages =
//            // tokenLimiter.adjustHistoryForAlternatingRoles(recentMessages);
//            // chatCompletion = baiduConfig.getRandomChatBuilder();
//
//            summary = chatCompletion.execute().getResult();
//
//            log.info("Refined title: " + summary);

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
