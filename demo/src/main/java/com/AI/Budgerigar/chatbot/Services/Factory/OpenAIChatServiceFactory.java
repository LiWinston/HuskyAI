package com.AI.Budgerigar.chatbot.Services.Factory;

import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenAIChatServiceFactory {

    @Autowired
    private AutowireCapableBeanFactory beanFactory; // Automatic injection.

    /**
     * Create an instance of OpenAIChatServiceImpl, let Spring automatically inject
     * dependencies, and only set the URL and model ID of OpenAI.
     * @param openAIUrl service URL
     * @param model model ID
     * @return Return an instance of OpenAIChatServiceImpl with automatically injected
     * dependencies.
     */
    public OpenAIChatServiceImpl create(String openAIUrl, String model) {
        OpenAIChatServiceImpl service = new OpenAIChatServiceImpl();

        // Inject other dependencies first.
        beanFactory.autowireBean(service);

        // Then set specific values.
        service.setModel(model);
        service.setOpenAIUrl(openAIUrl);
        service.setRestTemplate(new RestTemplate());

        return (OpenAIChatServiceImpl) beanFactory.initializeBean(service,
                "openAIChatService_" + openAIUrl + "_" + model);
        /*
         * 不必也不应使调用beanFactory.registerSingleton(beanName, proxy); 这是一个典型的手动管理生命周期的场景： 对象由
         * map 引用并管理 生命周期与 map 中的引用一致 移除时直接从 map 删除即可被 GC registerSingleton 反而可能带来问题： 会在
         * Spring 容器中保持对该对象的引用 即使从 map 中移除，如果没有显式从容器注销，对象仍然不会被 GC 可能造成内存泄露
         */
    }

    /**
     * Create an instance of OpenAIChatServiceImpl and let Spring automatically inject
     * dependencies to create and modify OpenAI's URL, model ID, and API key.
     * @param openAIUrl service URL
     * @param model model ID
     * @param openaiApiKey OpenAI API key
     * @return Return an instance of OpenAIChatServiceImpl with automatically injected
     * dependencies.
     */
    public OpenAIChatServiceImpl create(String openAIUrl, String model, String openaiApiKey) {
        OpenAIChatServiceImpl service = new OpenAIChatServiceImpl();

        // Inject other dependencies first.
        beanFactory.autowireBean(service);

        // Then set specific values.
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