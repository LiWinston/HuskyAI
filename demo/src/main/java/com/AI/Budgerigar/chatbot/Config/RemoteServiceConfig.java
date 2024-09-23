package com.AI.Budgerigar.chatbot.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Data
    public static class ServiceConfig {

        private String url;

        private String name; // 可选的别名

    }

}