package com.AI.Budgerigar.chatbot.chatbotAdmin.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
public class ModelsStatusVO {

    private List<ModelService> modelServices;

    @Data
    @Builder
    @ToString
    public static class ModelService {

        private String url;

        private String name;

        private String apiKey;

        private List<ModelStatus> mdList;

        @Data
        @Builder
        @ToString
        @AllArgsConstructor
        public static class ModelStatus {

            private String model;

            private Boolean allowed = true;

            private Boolean availableFromServer;

            public ModelStatus(String model, Boolean availableFromServer) {
                this.model = model;
                this.allowed = true;
                this.availableFromServer = availableFromServer;
            }

        }

    }

}
