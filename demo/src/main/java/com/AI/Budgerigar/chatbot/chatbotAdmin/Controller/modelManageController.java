package com.AI.Budgerigar.chatbot.chatbotAdmin.Controller;

import com.AI.Budgerigar.chatbot.Config.RemoteServiceConfig;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.chatServicesManageService;
import com.AI.Budgerigar.chatbot.chatbotAdmin.VO.ModelsStatusVO;
import com.AI.Budgerigar.chatbot.result.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model management controller.
 * <p>
 * Features：
 * <ul>
 * <li>Model list: GET /admin/models -> modelsStatusVO</li>
 * <li>Model details and enable/disable:
 * <ul>
 * <li>Detail: Use the result of the above GET request.</li>
 * <li>Enable/Disable: POST /admin/models，body: {name, model, operation}. "operation" can be "enable" or
 * "disable"</li>
 * </ul>
 * </li>
 * <li>Dynamically register a new model: POST /admin/models，body: registerModelDTO，return Result&lt;Boolean&gt;</li>
 * <li>Manually refresh the model: POST /admin/models/refresh</li>
 * </ul>
 */

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/models")
@Slf4j
public class modelManageController {

    @Autowired
    private chatServicesManageService chatServicesManageService;

    @Autowired
    private RemoteServiceConfig remoteServiceConfig;

    /**
     * Get the model list, "RemoteServiceConfig" records all the service configurations, including the service's URL, name, apiKey, allowedModels.
     * ChatServicesManageService manages and maintains all currently available services, especially ConcurrentHashMap<String,
     * ConcurrentHashMap<String, ChatService>>
     * chatServices record name - (model-service instance) mapping. The name may be determined as "name" or "url" depending on the configuration.
     * To obtain a stable URL, you can use the service instance getOpenAIUrl method (if the service instance is OpenAIChatServiceImpl).
     * Please note that Baidu and Doubao are not handled here, as they do not support the OpenAI interface. If you need to make changes, please modify the configuration.
     * The returned ModelsStatusVO records the URL, name, apiKey of all services, and the mdList, which records the model name, whether access is allowed, and whether it is available on the server.
     * @return Model list
     */
    @GetMapping
    public Result<ModelsStatusVO> getModelsStatus() {
        try {

            ConcurrentHashMap<String, ConcurrentHashMap<String, ChatService>> availableServicesHashMap = chatServicesManageService
                .getChatServices();
            List<RemoteServiceConfig.ServiceConfig> serviceConfigs = remoteServiceConfig.getServiceConfigs();

            ModelsStatusVO.ModelsStatusVOBuilder modelsStatusVOBuilder = ModelsStatusVO.builder();
            List<ModelsStatusVO.ModelService> modelServiceList = new ArrayList<>();
            for (RemoteServiceConfig.ServiceConfig serviceConfig : serviceConfigs) {
                String name = serviceConfig.getName();
                if (name.equals("baidu") || name.equals("豆包")) {
                    continue;
                }
                String url = serviceConfig.getUrl();
                String apiKey = serviceConfig.getApiKey();
                List<String> allowedModels = serviceConfig.getAllowedModels();

                // Check the availableServicesHashMap to see if it is present, and if it is, it is available.
                ConcurrentHashMap<String, ChatService> chatServiceHashMap = availableServicesHashMap
                    .get(!name.isBlank() ? name : url);
                ModelsStatusVO.ModelService.ModelServiceBuilder modelServiceBuilder = ModelsStatusVO.ModelService
                    .builder();

                List<ModelsStatusVO.ModelService.ModelStatus> modelStatusList = new ArrayList<>();
                for (String model : ((null == allowedModels || allowedModels.isEmpty()) ? chatServiceHashMap.keySet()
                        : allowedModels)) {
                    // if(!chatServiceHashMap.containsKey(model)){
                    // continue;
                    // }
                    Boolean availableFromServer = chatServiceHashMap.containsKey(model);
                    ModelsStatusVO.ModelService.ModelStatus.ModelStatusBuilder modelStatusBuilder = ModelsStatusVO.ModelService.ModelStatus
                        .builder();
                    modelStatusBuilder.model(model).allowed(true).availableFromServer(availableFromServer);
                    modelStatusList.add(modelStatusBuilder.build());
                }
                modelServiceBuilder.url(url).name(name).apiKey(apiKey).mdList(modelStatusList);
                modelServiceList.add(modelServiceBuilder.build());
            }

            modelsStatusVOBuilder.modelServices(modelServiceList);
            return Result.success(modelsStatusVOBuilder.build());

        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", modelManageController.class.getName(), e.getMessage());
            return Result.error("Failed to obtain the model list.");
        }
    }

    @PostMapping("/refresh")
    public Result<refreshResultDTO> refreshModels() {
        try {
            Result<AbstractMap.SimpleEntry<Integer, Integer>> var = chatServicesManageService
                .checkRemoteServicesHealth()
                .get();
            return var.getCode() == 1 ? Result
                .success(new refreshResultDTO(var.getData().getKey(), var.getData().getValue()), var.getMsg())
                    : Result.error(var.getMsg());
        }
        catch (Exception e) {
            log.error("Error occurred in {}: {}", modelManageController.class.getName(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    public static class refreshResultDTO {

        private Integer add;

        private Integer remove;

    }

}