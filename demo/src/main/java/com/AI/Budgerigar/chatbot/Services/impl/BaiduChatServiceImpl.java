package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BaiduChatServiceImpl implements ChatService {

    @Autowired
    private Qianfan qianfan;

    @Autowired
    private BaiduConfig baiduConfig;

    private List<String[]> conversationHistory;

    // Declare a static Logger object for logging within this class.
    private static final Logger logger = Logger.getLogger(BaiduChatServiceImpl.class.getName());

    // Initialize a fixed greeting for the user.
    private static final String GREETING_USER = "Hello";
    // Initialize a fixed greeting for the assistant in response to the user.
    private static final String GREETING_ASSISTANT = "Hello! What can I do for you?";

    @PostConstruct
    public void init() {
        // Initialize the conversation history list.
        conversationHistory = new ArrayList<>();
        // Add the initial greeting from the user to the conversation history.
        conversationHistory.add(new String[]{"user", GREETING_USER});
        // Add the initial greeting response from the assistant to the conversation history.
        conversationHistory.add(new String[]{"assistant", GREETING_ASSISTANT});
    }

    @Override
    public String chat(String input) {
        try {
            // Add the user's input to the conversation history.
            conversationHistory.add(new String[]{"user", input});

            // Create a ChatCompletion object and configure it with the model from BaiduConfig.
            var chatCompletion = qianfan.chatCompletion()
                    .model(baiduConfig.getModel());

            // Add each message from the conversation history to the ChatCompletion request.
            for (String[] message : conversationHistory) {
                chatCompletion.addMessage(message[0], message[1]);
            }

            // Execute the ChatCompletion request and get the response.
            ChatResponse response = chatCompletion.execute();

            // Get the result from the response.
            String result = response.getResult();
            // Log the result for debugging purposes.
            logInfo(result);

            // Add the model's response to the conversation history.
            conversationHistory.add(new String[]{"assistant", result});

            // Return the result to the caller.
            return result;
        } catch (Exception e) {
            // Log any exceptions that occur during the chat processing.
            logger.log(Level.SEVERE, "Error occurred in " + BaiduChatServiceImpl.class.getName() + ": " + e.getMessage(), e);
            // Throw a RuntimeException to indicate an error in processing the chat request.
            throw new RuntimeException("Error processing chat request", e);
        }
    }

    public void logInfo(String message) {
        // Log an information message with a prefix indicating the class name.
        logger.info(BaiduChatServiceImpl.class.getName() + " : " + message.substring(0, Math.min(20, message.length())));
    }
}