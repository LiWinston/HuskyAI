package com.AI.Budgerigar.chatbot.Controller.Utils;

import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 用户名建议生成工具类 Utility class for generating username suggestions
 */
public class UsernameSuggestionUtil {

    private static final int MAX_SUGGESTIONS = 3;

    private static final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    /**
     * 根据用户名生成建议的候选用户名 Generate username suggestions based on Levenshtein distance
     * @param userMapper 数据库查询映射器
     * @param username 输入的用户名
     * @return List<String> 建议的用户名列表
     */
    public static List<String> generateUsernameSuggestions(UserMapper userMapper, String username) {
        List<String> candidates = new ArrayList<>();

        // Step 1: 生成各种候选项
        candidates.add(username.toLowerCase());
        candidates.add(username.toUpperCase());
        candidates.add(capitalizeFirstLetter(username));

        if (!username.contains("_")) {
            candidates.add(insertUnderscore(username, 1));
            candidates.add(insertUnderscore(username, username.length() / 2));
        }

        candidates.add(username + "1");
        candidates.add(username + "_01");
        candidates.add(username + "123");

        // Step 2: 确保建议项在数据库中是唯一的
        List<String> validCandidates = new ArrayList<>();
        for (String candidate : candidates) {
            if (userMapper.getUserByUsername(candidate) == null) {
                validCandidates.add(candidate);
            }
        }

        // Step 3: 根据 Levenshtein 距离进行排序
        validCandidates.sort(Comparator.comparingInt(c -> levenshteinDistance.apply(username, c)));

        // Step 4: 返回前 N 个建议项
        return validCandidates.subList(0, Math.min(MAX_SUGGESTIONS, validCandidates.size()));
    }

    // Capitalize first letter of the username
    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    // Insert underscore at a specific position in the username
    private static String insertUnderscore(String input, int position) {
        if (position < 0 || position > input.length()) {
            return input;
        }
        return new StringBuilder(input).insert(position, "_").toString();
    }

}
