package com.AI.Budgerigar.chatbot.Controller.Utils;

import com.AI.Budgerigar.chatbot.mapper.UserMapper;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for generating username suggestions
 */
public class UsernameSuggestionUtil {

    private static final int MAX_SUGGESTIONS = 3;

    private static final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    /**
     * Generate username suggestions based on Levenshtein distance
     * @param userMapper Database query mapper.
     * @param username Input username.
     * @return List<String> Suggested username list.
     */
    public static List<String> generateUsernameSuggestions(UserMapper userMapper, String username) {
        List<String> candidates = new ArrayList<>();

        // Step 1: Generate various candidates.
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

        // Step 2: Ensure that the suggested item is unique in the database.
        List<String> validCandidates = new ArrayList<>();
        for (String candidate : candidates) {
            if (userMapper.getUserByUsername(candidate) == null) {
                validCandidates.add(candidate);
            }
        }

        // Step 3: Sort by Levenshtein distance.
        validCandidates.sort(Comparator.comparingInt(c -> levenshteinDistance.apply(username, c)));

        // Step 4: Return the top N suggestions.
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
