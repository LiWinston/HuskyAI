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