package com.AI.Budgerigar.chatbot.Services.Factory;

import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenAIChatServiceFactory {

    @Autowired
    private AutowireCapableBeanFactory beanFactory; // 自动注入

    /**
     * 创建 OpenAIChatServiceImpl 实例，并让 Spring 自动注入依赖，只设置 OpenAI 的 URL 和模型 ID
     * @param openAIUrl 服务的 URL
     * @param model 模型 ID
     * @return 返回一个自动注入依赖的 OpenAIChatServiceImpl 实例
     */
    public OpenAIChatServiceImpl create(String openAIUrl, String model) {
        OpenAIChatServiceImpl service = new OpenAIChatServiceImpl();

        // 先注入其他依赖
        beanFactory.autowireBean(service);

        // 然后设置特定的值
        service.setModel(model);
        service.setOpenAIUrl(openAIUrl);
        service.setRestTemplate(new RestTemplate());

        return (OpenAIChatServiceImpl) beanFactory.initializeBean(service,
                "openAIChatService_" + openAIUrl + "_" + model);
    }

    /**
     * 创建 OpenAIChatServiceImpl 实例，并让 Spring 自动注入依赖，用于创建并自行修改 OpenAI 的 URL、模型 ID 和 API 密钥
     * @param openAIUrl 服务的 URL
     * @param model 模型 ID
     * @param openaiApiKey OpenAI 的 API 密钥
     * @return 返回一个自动注入依赖的 OpenAIChatServiceImpl 实例
     */
    public OpenAIChatServiceImpl create(String openAIUrl, String model, String openaiApiKey) {
        OpenAIChatServiceImpl service = new OpenAIChatServiceImpl();

        // 先注入其他依赖
        beanFactory.autowireBean(service);

        // 然后设置特定的值
        service.setModel(model);
        service.setOpenAIUrl(openAIUrl);
        service.setOpenaiApiKey(openaiApiKey);
        service.setRestTemplate(new RestTemplate() {
            {
                getInterceptors().add((request, body, execution) -> {
                    request.getHeaders().add("Authorization", "Bearer " + openaiApiKey);
                    return execution.execute(request, body);
                });
            }
        });

        return (OpenAIChatServiceImpl) beanFactory.initializeBean(service,
                "openAIChatService_" + openAIUrl + "_" + model);
    }

}