package com.AI.Budgerigar.chatbot.Config;

import com.baidubce.qianfan.Qianfan;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Setter
@Getter
public class BaiduConfig {

    @Value("${baidu.api.key}")
    private String apiKey;

    @Value("${baidu.api.secret}")
    private String apiSecret;

//    @Value("${baidu.api.url}")
//    private String apiUrl;

    @Value("${baidu.api.model}")
    private String model;

    @Bean
    @Qualifier("qianfan")
    public Qianfan qianfan() {
        // 初始化Qianfan配置
        BaiduConfig config = new BaiduConfig();
        config.setApiKey(apiKey);
        config.setApiSecret(apiSecret);
        config.setModel(model);

        // 创建并返回Qianfan实例
        return new Qianfan(
                config.getApiKey(),
                config.getApiSecret()
        );
    }
}
