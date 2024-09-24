package com.AI.Budgerigar.chatbot.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 读取Spring Boot配置文件中的远程服务配置
 */
@Component
@ConfigurationProperties(prefix = "remote-services")
public class RemoteServiceConfig {

    private List<ServiceConfig> services;

    public List<ServiceConfig> getServices() {
        return services;
    }

    public void setServices(List<ServiceConfig> services) {
        this.services = services;
    }

    /**
     * 服务器视角的模型访问配置，此处不配置的模型不注册、不可访问
     */
    @Data
    public static class ServiceConfig {

        private String url;

        /**
         * 可选的别名，用于View和控制器
         */
        private String name;

        /**
         * 可选的 API 密钥，当该url需要时设置
         */
        private String apiKey;

        /**
         * 服务器视角的模型访问限制，全局生效，例如： { "allowedModels": ["model1", "model2"] }
         */
        private List<String> allowedModels;

    }

}