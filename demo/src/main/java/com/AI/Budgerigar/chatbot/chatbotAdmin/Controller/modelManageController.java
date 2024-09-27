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
 * 模型管理
 * <p>
 * 功能：
 * <ul>
 * <li>模型列表: GET /admin/models -> modelsStatusVO</li>
 * <li>模型详情和启用/禁用:
 * <ul>
 * <li>详情: 使用上述 GET 请求的结果</li>
 * <li>启用/禁用: POST /admin/models，body: {name, model, operation}，其中 operation 可为 "enable" 或
 * "disable"</li>
 * </ul>
 * </li>
 * <li>动态注册新模型: POST /admin/models，body: registerModelDTO，返回 Result&lt;Boolean&gt;</li>
 * <li>手动刷新模型: POST /admin/models/refresh</li>
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
     * 获取模型列表, RemoteServiceConfig记录了所有的服务配置，包括服务的url，name，apiKey，allowedModels
     * ChatServicesManageService管理维护了所有的当前可用服务,尤其是ConcurrentHashMap<String,
     * ConcurrentHashMap<String, ChatService>>
     * chatServices记录了名-（模型-服务实例）的映射，名可能为name或者url视配置而定，若需获得稳妥url，可使用服务实例getOpenAIUrl方法(若服务实例为OpenAIChatServiceImpl)
     * 注意此处不处理baidu和豆包，因为其不支持openai接口，如需修改请改配置
     * 返回的ModelsStatusVO记录了所有服务的url，name，apiKey，mdList，mdList记录了模型名，是否允许访问，是否在服务器可用
     * @return 模型列表
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

                // 到availableServicesHashMap中探查其是否在，若在即为可用
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
            return Result.error("获取模型列表失败");
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