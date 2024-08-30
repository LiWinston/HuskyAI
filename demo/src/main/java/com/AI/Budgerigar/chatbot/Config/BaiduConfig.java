package com.AI.Budgerigar.chatbot.Config;

import com.baidubce.qianfan.Qianfan;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@Setter
@Getter
public class BaiduConfig {

    @Value("${baidu.api.key}")
    private String apiKey;

    @Value("${baidu.api.secret}")
    private String apiSecret;

    @Value("${baidu.api.models}")
    private String models; // 读取所有模型的配置，以逗号分隔

    private List<String> modelList = new ArrayList<>();
    private int currentModelIndex = 0;

    @Bean
    @Qualifier("qianfan")
    public Qianfan qianfan() {
        // 初始化Qianfan配置
        return new Qianfan(apiKey, apiSecret);
    }

    @PostConstruct
    public void init() {
        // 初始化模型列表
        modelList = Arrays.asList(models.split(","));
    }

    // 获取当前使用的模型
    public String getCurrentModel() {
        return modelList.get(currentModelIndex);
    }

    // 切换到下一个模型
    public void switchToNextModel() {
        currentModelIndex = (currentModelIndex + 1) % modelList.size();
    }
}
