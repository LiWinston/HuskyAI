package com.AI.Budgerigar.chatbot.AIUtil;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.min;

@Component
@Slf4j
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
        }
        else {
            return getFixedHistory(conversationId);
        }
    }

    public List<String[]> getAdaptiveHistory(String conversationId, int maxTokens) {
        LinkedList<String[]> adaptiveHistory = new LinkedList<>();
        int tokenCount = 0;
        int batchSize = 8; // Initial batch read quantity.

        int finalMaxTokens = min(maxTokenLimit, maxTokens);
        // log.info("maxTokens: {}, finalMaxTokens: {},maxTokenLimit: {}", maxTokens,
        // finalMaxTokens, maxTokenLimit);
        long messageCount = chatMessagesRedisDAO.getMessageCount(conversationId);
        int startIndex = (int) messageCount - 1;

        while (startIndex >= 0 && tokenCount < finalMaxTokens) {
            int endIndex = min(startIndex, (int) messageCount - 1);
            int actualStartIndex = Math.max(0, endIndex - batchSize + 1);
            List<String[]> batchMessages = chatMessagesRedisDAO.getMessagesRange(conversationId, actualStartIndex,
                    endIndex);

            if (batchMessages.isEmpty()) {
                break;
            }

            boolean reachedLimit = false;
            for (int i = batchMessages.size() - 1; i >= 0; i--) {
                String[] message = batchMessages.get(i);
                String content = message[2];
                int messageTokens = estimateTokenCount(content);

                if (tokenCount + messageTokens <= finalMaxTokens) {
                    tokenCount += messageTokens;
                    adaptiveHistory.addFirst(message);
                }
                else {
                    reachedLimit = true;
                    break;
                }
            }

            if (reachedLimit) {
                break;
            }

            batchSize *= 2;
            startIndex = actualStartIndex - 1;
        }

        // Adjust the history to ensure the correct order and role alternation.
        return adjustHistoryForAlternatingRoles(adaptiveHistory);
    }

    // Fixed: Obtain conversation history based on the message count limit.
    public List<String[]> getFixedHistory(String conversationId) {
        long totalMessages = chatMessagesRedisDAO.getMessageCount(conversationId);
        int messageCount = min(maxMessageLimit, (int) totalMessages);

        if (messageCount % 2 == 0) {
            messageCount++;
        }

        List<String[]> fixedHistory = chatMessagesRedisDAO.getLastNMessages(conversationId, messageCount);

        // Stricter adjustment: ensure alternate order.
        adjustHistoryForAlternatingRoles(fixedHistory);

        return fixedHistory;
    }

    public List<String[]> getFixedHistory(String conversationId, int maxMessageLimit) {
        long totalMessages = chatMessagesRedisDAO.getMessageCount(conversationId);
        int messageCount = min(maxMessageLimit, (int) totalMessages);

        if (messageCount % 2 == 0) {
            messageCount++;
        }

        List<String[]> fixedHistory = chatMessagesRedisDAO.getLastNMessages(conversationId,
                min(maxMessageLimit, messageCount));

        // Stricter adjustment: ensure alternate order.
        adjustHistoryForAlternatingRoles(fixedHistory);

        return fixedHistory;
    }

    // Ensure history starts with and ends with "user". User and assistant messages alternate.
    public List<String[]> adjustHistoryForAlternatingRoles(List<String[]> history) {
        if (history.isEmpty()) {
            return history;
        }

        LinkedList<String[]> adjustedHistory = new LinkedList<>();
        String expectedRole = "user";

        // Start processing from the latest news, ensuring it ends with the user's message.
        for (int i = history.size() - 1; i >= 0; i--) {
            String[] message = history.get(i);
            if (message[0].equals(expectedRole)) {
                adjustedHistory.addFirst(message);
                expectedRole = expectedRole.equals("user") ? "assistant" : "user";
            }
        }

        // If the adjusted history does not start with "user", remove the first message.
        if (!adjustedHistory.isEmpty() && !"user".equals(adjustedHistory.getFirst()[0])) {
            adjustedHistory.removeFirst();
        }

        // Ensure the number of messages is odd.
        if (adjustedHistory.size() % 2 == 0 && !adjustedHistory.isEmpty()) {
            adjustedHistory.removeFirst();
        }

        return adjustedHistory;
    }

    private static Pattern TOKEN_PATTERN;

    @PostConstruct
    public void init() {
        // Initialize "TOKEN_PATTERN".
        TOKEN_PATTERN = Pattern.compile("\\w+|\\p{Punct}|\\s+");
    }

    // An auxiliary method to accurately estimate the number of tokens in a message.
    public int estimateTokenCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // Use the pre-compiled "TOKEN_PATTERN".
        Matcher matcher = TOKEN_PATTERN.matcher(content);

        int tokenCount = 0;
        while (matcher.find()) {
            tokenCount++;
        }

        return tokenCount;
    }

}
