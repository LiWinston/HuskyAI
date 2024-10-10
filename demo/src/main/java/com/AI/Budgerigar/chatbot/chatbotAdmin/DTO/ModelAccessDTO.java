package com.AI.Budgerigar.chatbot.chatbotAdmin.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.Map;

/**
 * ModelAccessDTO class represents the DTO for transferring model access data.
 */
@Data
@NoArgsConstructor
public class ModelAccessDTO {
    private String url;
    private String model;
    private String accessLevel = "full";
    private AccessRestrictionDTO accessRestriction;
    private Integer priority = 0;
    private Map<String, Object> additionalAttributes;

    @Data
    @NoArgsConstructor
    public static class AccessRestrictionDTO {
        private Date startTime;
        private Date endTime;
        private Boolean timeRestricted = false;
        private Integer maxDailyAccess;
    }
}
