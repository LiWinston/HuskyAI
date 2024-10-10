package com.AI.Budgerigar.chatbot.Config;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.core.builder.ChatBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Configuration
@Setter
@Getter
@Slf4j
public class BaiduConfig {

    @Value("${baidu.api.key}")
    private String apiKey;

    @Value("${baidu.api.secret}")
    private String apiSecret;

    @Value("${baidu.api.models}")
    private String models; // Read the configuration of all models, separated by commas.

    private List<String> modelList = new ArrayList<>();

    private int currentModelIndex = 0;

    @Autowired
    private Qianfan qianfan;

    private ChatBuilder currentChatBuilder;

    @Bean
    @Qualifier("qianfan")
    public Qianfan qianfan() {
        // Initialize Qianfan configuration.
        return new Qianfan(apiKey, apiSecret);
    }

    @PostConstruct
    public void init() {
        Random Random = new Random();
        // Initialize the model list.
        modelList = Arrays.asList(models.split(","));
        currentModelIndex = Random.nextInt(modelList.size());
        log.info("INIT BaiduConfig currentModelIndex: {}{}", currentModelIndex, modelList.get(currentModelIndex));
    }

    // Obtain the current model.
    public String getCurrentModel() {
        return modelList.get(currentModelIndex);
    }

    private String getRandomModel() {
        int randomIndex = (int) (Math.random() * modelList.size());// Ensure the random number is within the range of the model list.
        return modelList.get(randomIndex);
    }

    private String getRandomDifferentModel() {
        int randomIndex = (int) (Math.random() * modelList.size());
        while (randomIndex == currentModelIndex) {
            randomIndex = (int) (Math.random() * modelList.size());
        }
        return modelList.get(randomIndex);
    }

    // Change to the next model.
    public void switchToNextModel() {
        currentModelIndex = (currentModelIndex + 1) % modelList.size();
        currentChatBuilder = qianfan.chatCompletion().model(getCurrentModel());
    }

    public ChatBuilder getCurrentChatBuilder() {
        return currentChatBuilder == null ? qianfan.chatCompletion().model(getCurrentModel()) : currentChatBuilder;
    }

    public ChatBuilder getRandomChatBuilder() {
        return qianfan.chatCompletion().model(getRandomModel());
    }

    public ChatBuilder getRandomDifferentChatBuilder() {
        return qianfan.chatCompletion().model(getRandomDifferentModel());
    }

    public ChatBuilder switchModel() {
        switchToNextModel();
        return getCurrentChatBuilder();
    }

}
