package com.AI.Budgerigar.chatbot.Config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Read the remote service configuration from the Spring Boot configuration file.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "remote-services")
public class RemoteServiceConfig {

    private List<ServiceConfig> serviceConfigs;

    /**
     * Model access configuration from the server's perspective; models not configured here will not be registered or accessible.
     */
    @Data
    public static class ServiceConfig {

        private String url;

        /**
         * Optional alias for View and controller.
         */
        private String name;

        /**
         * Optional API key, set when the URL requires it.
         */
        private String apiKey;

        /**
         * Model access restrictions from the server's perspective, globally effective, e.g., { "allowedModels": ["model1", "model2"] }.
         */
        private List<String> allowedModels;

    }

}