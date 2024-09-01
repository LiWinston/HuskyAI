package com.AI.Budgerigar.chatbot.AIUtil;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class TokenLimiter {

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Autowired
    private ChatService.TokenLimitType tokenLimitType;

    @Value("${chatbot.maxTokenLimit:800}")
    private Integer maxTokenLimit;

    @Value("${chatbot.maxMessageLimit:15}")
    private Integer maxMessageLimit;

    public synchronized List<String[]> getAdaptiveConversationHistory(String conversationId, int maxTokens) {
        if (tokenLimitType == ChatService.TokenLimitType.Adaptive) {
            return getAdaptiveHistory(conversationId, maxTokens);
        } else {
            return getFixedHistory(conversationId);
        }
    }

    // 自适应：根据 token 总量限制从后向前获取对话历史
    private List<String[]> getAdaptiveHistory(String conversationId, int maxTokens) {
        LinkedList<String[]> adaptiveHistory = new LinkedList<>();
        int tokenCount = 0;
        int batchSize = 8; // 初始批量读取8条消息，可以根据需要调整

        int finalMaxTokens = Math.min(maxTokenLimit, maxTokens);
        long messageCount = chatMessagesRedisDAO.getMessageCount(conversationId);
        int startIndex = (int) messageCount - 1; // 从最后一条消息开始
        int endIndex = startIndex;
        // 从后向前逐步读取，直到满足条件或到达历史的开头
        while (startIndex >= 0) {

            startIndex = Math.max(startIndex - batchSize + 1, 0); // 向前读取batchSize条消息
            List<String[]> batchMessages = chatMessagesRedisDAO.getMessagesRange(conversationId, startIndex, endIndex);

            // 如果 batchMessages 是空的，结束循环
            if (batchMessages.isEmpty()) {
                break;
            }

            // 逆序遍历消息
            for (int i = batchMessages.size() - 1; i >= 0; i--) {
                String[] message = batchMessages.get(i);
                String content = message[2];
                int messageTokens = estimateTokenCount(content);

                // 检查是否会超出 token 限制，如果没有超出则添加
                if (tokenCount + messageTokens <= finalMaxTokens) {
                    tokenCount += messageTokens;
                    adaptiveHistory.addFirst(message); // 小批次内顺序遍历
                } else {
                    // 如果超出限制，检查是否允许读取更多
                    if (adaptiveHistory.isEmpty()) {
                        adaptiveHistory.addFirst(message); // 没有其他消息时允许添加单条超限消息
                    }
                    // 超出 token 限制，停止累加
                    break;
                }
            }

            // 增加批次大小，以便下一轮读取更多消息
            batchSize += 2;

            // 检查是否已满足token数量条件或达到历史记录的开头
            if (tokenCount >= finalMaxTokens || startIndex <= 0) {
                break;
            }

            endIndex = startIndex - 1; // 更新 endIndex 以读取下一批消息
            startIndex = startIndex - batchSize;  // 更新 startIndex 向前读取更多消息
        }

        // 确保历史记录以正确的顺序排列
        adjustHistoryForAlternatingRoles(adaptiveHistory);

        return adaptiveHistory;
    }



    // 固定：根据消息条数限制获取对话历史
    private List<String[]> getFixedHistory(String conversationId) {
        long totalMessages = chatMessagesRedisDAO.getMessageCount(conversationId);
        int messageCount = Math.min(maxMessageLimit, (int) totalMessages);

        if (messageCount % 2 == 0) {
            messageCount++;
        }

        List<String[]> fixedHistory = chatMessagesRedisDAO.getLastNMessages(conversationId, messageCount);

        // 更严格的调整：确保交替顺序
        adjustHistoryForAlternatingRoles(fixedHistory);

        return fixedHistory;
    }

    // 确保历史以"user"开始和结束，且用户和助手消息交替
    private void adjustHistoryForAlternatingRoles(List<String[]> history) {
        // 确保历史以"user"开始
        while (!history.isEmpty() && !"user".equals(history.get(0)[0])) {
            history.remove(0);
        }

        // 确保历史交替，且以"user"结束
        for (int i = 0; i < history.size(); i++) {
            String expectedRole = (i % 2 == 0) ? "user" : "assistant";
            if (!expectedRole.equals(history.get(i)[0])) {
                history.subList(i, history.size()).clear(); // 清理不符合顺序的部分
                break;
            }
        }

        // 如果历史是偶数条，移除最后一条消息以确保为奇数条
        if (history.size() % 2 == 0 && !history.isEmpty()) {
            history.remove(history.size() - 1);
        }

        // 最终检查，如果经过调整后历史列表为空或不满足要求，直接返回空列表
        if (history.isEmpty() || !"user".equals(history.get(0)[0]) || !"user".equals(history.get(history.size() - 1)[0])) {
            history.clear();
        }
    }

    // 估算消息的token数量的辅助方法
    private int estimateTokenCount(String content) {
        return content.split("\\s+").length;
    }
}
