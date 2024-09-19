package com.AI.Budgerigar.chatbot.Services.Factory;

import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenAIChatServiceFactory {

    @Autowired
    private AutowireCapableBeanFactory beanFactory; // 自动注入

    /**
     * 创建 OpenAIChatServiceImpl 实例，并让 Spring 自动注入依赖
     * @param openAIUrl 服务的 URL
     * @param model 模型 ID
     * @return 返回一个自动注入依赖的 OpenAIChatServiceImpl 实例
     */
    public OpenAIChatServiceImpl create(String openAIUrl, String model) {
        // 手动传递 openAIUrl 和 model 构造实例
        OpenAIChatServiceImpl service = new OpenAIChatServiceImpl(openAIUrl, model);

        // 使用 Spring 的 AutowireCapableBeanFactory 来注入其他依赖
        beanFactory.autowireBean(service);

        return service;
    }

}