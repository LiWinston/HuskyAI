package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.AIUtil.TokenLimiter;
import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.Nosql.ChatMessagesMongoDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.ChatResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

@Service
@Setter
@Slf4j
public class BaiduChatServiceImpl implements ChatService {

    private static final Logger logger = Logger.getLogger(BaiduChatServiceImpl.class.getName());

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private TokenLimiter tokenLimiter;

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
        log.info("FE SET conversation ID to: " + conversationId);
    }

    @Getter
    public String conversationId;
    @Autowired
    DateTimeFormatter dateTimeFormatter;
    @Autowired
    private Qianfan qianfan;
    @Autowired
    private BaiduConfig baiduConfig;
    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;
//    // 使用 ThreadLocal 来存储 conversationId
//    private static final ThreadLocal<String> conversationIdThreadLocal = ThreadLocal.withInitial(() -> "default_baidu_conversation");
    @Autowired
    private ChatMessagesMongoDAO chatMessagesMongoDAO;
    @Autowired
    private ChatSyncService chatSyncService;

    @Autowired
    TokenLimitType tokenLimitType;

    String getNowTimeStamp() {
        return Instant.now().toString().formatted(dateTimeFormatter);
    }


    @PostConstruct
    public void init() {
        conversationId = "default_baidu_conversation"; // This would typically be dynamic per session/user
//        chatMessagesRedisDAO.addMessage(conversationId, "user", "Hi");
//        chatMessagesRedisDAO.addMessage(conversationId, "assistant", "What can I do for U?");
    }

    @Override
    public String chat(String input) {
        try {
            chatSyncService.updateRedisFromMongo(conversationId);

            chatMessagesRedisDAO.maintainMessageHistory(conversationId);
            // 添加用户输入到 Redis 对话历史
            chatMessagesRedisDAO.addMessage(conversationId, "user", getNowTimeStamp(), StringEscapeUtils.escapeHtml4(input));

            // 创建 ChatCompletion 请求对象
            var chatCompletion = qianfan.chatCompletion().model(baiduConfig.getCurrentModel());

            // 从 Redis 中获取对话历史
            List<String[]> conversationHistory = null;
            try{
                conversationHistory = tokenLimiter.getAdaptiveConversationHistory(conversationId,1800);
                log.info("自适应缩放到" + conversationHistory.size() + "条消息");
                for (String[] entry : conversationHistory) {
                    log.info("{} : {}", entry[0], entry[2].substring(0, Math.min(20, entry[2].length())));
                }
            }catch (Exception e){
                log.error("Error occurred in {}: {}", TokenLimiter.class.getName(), e.getMessage(), e);
                chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(), "Query failed. Please try again.");
                throw new RuntimeException("Error processing chat request", e);
            }

            // 添加对话历史到请求对象中
            for (String[] entry : conversationHistory) {
                chatCompletion.addMessage(entry[0], entry[2]); // entry[0] 是角色，entry[2] 是内容
            }

            // 执行请求
            ChatResponse response = chatCompletion.execute();
            String result = response.getResult();
            logInfo(" # " + baiduConfig.getCurrentModel() + "\n" + result);

            // 将助手的响应添加到 Redis 对话历史
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(), StringEscapeUtils.escapeHtml4(result));

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

            return result;
        } catch (Exception e) {
            log.error("Error occurred in {}: {}", BaiduChatServiceImpl.class.getName(), e.getMessage(), e);
            chatMessagesRedisDAO.addMessage(conversationId, "assistant", getNowTimeStamp(), "Query failed. Please try again.");
            throw new RuntimeException("Error processing chat request", e);
        }
    }

    private int getMongoConversationLength(String conversationId) {
        // Implement the logic to get the conversation length from MongoDB
        // Assuming ChatMessagesMongoDAO has a method to get the conversation length
        return chatMessagesMongoDAO.getConversationLengthById(conversationId);
    }

}
