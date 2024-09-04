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
    private String models; // 读取所有模型的配置，以逗号分隔

    private List<String> modelList = new ArrayList<>();

    private int currentModelIndex = 0;

    @Autowired
    private Qianfan qianfan;

    private ChatBuilder currentChatBuilder;

    @Bean
    @Qualifier("qianfan")
    public Qianfan qianfan() {
        // 初始化Qianfan配置
        return new Qianfan(apiKey, apiSecret);
    }

    @PostConstruct
    public void init() {
        Random Random = new Random();
        // 初始化模型列表
        modelList = Arrays.asList(models.split(","));
        currentModelIndex = Random.nextInt(modelList.size());
        log.info("INIT BaiduConfig currentModelIndex: {}{}", currentModelIndex, modelList.get(currentModelIndex));
    }

    // 获取当前使用的模型
    public String getCurrentModel() {
        return modelList.get(currentModelIndex);
    }

    private String getRandomModel() {
        int randomIndex = (int) (Math.random() * modelList.size());// 确保随机数在模型列表范围内
        return modelList.get(randomIndex);
    }

    private String getRandomDifferentModel() {
        int randomIndex = (int) (Math.random() * modelList.size());
        while (randomIndex == currentModelIndex) {
            randomIndex = (int) (Math.random() * modelList.size());
        }
        return modelList.get(randomIndex);
    }

    // 切换到下一个模型
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
