package com.AI.Budgerigar.chatbot.Nosql;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * UserModelAccessConfig class represents the configuration for user model access.
 */
@Data
@Document(collection = "user_model_access_configs")
public class UserModelAccessConfig {

    @Id
    private String userId;

    /**
     * List of allowed model accesses.
     */
    private List<ModelAccess> allowedModels;

    /**
     * Version number of the access control configuration.
     */
    private int version = 1;

    /**
     * Maximum daily accesses allowed.
     */
    private Integer maxDailyAccesses;

    /**
     * Expiration date of the configuration (optional).
     */
    private Date expirationDate;

    /**
     * Indicates whether the configuration is disabled.
     */
    private Boolean disabled = false;

    /**
     * Additional metadata for future extensions.
     */
    private Map<String, Object> metadata;

    /**
     * ModelAccess class represents the access details for a specific model.
     */
    @Data
    public static class ModelAccess {

        /**
         * URL of the model.
         */
        private String url;

        /**
         * Name of the model.
         */
        private String model;

        /**
         * Access level of the model (e.g., read-only, full access).
         */
        private String accessLevel;

        /**
         * Additional access restrictions for the model.
         */
        private AccessRestriction accessRestriction;

        /**
         * Priority of the model access.
         */
        private Integer priority;

        /**
         * Additional attributes for future use.
         */
        private Map<String, Object> additionalAttributes;

    }

    /**
     * AccessRestriction class represents the restrictions on model access.
     */
    @Data
    public static class AccessRestriction {

        /**
         * Start time for access.
         */
        private Date startTime;

        /**
         * End time for access.
         */
        private Date endTime;

        /**
         * Indicates whether access is restricted to specific time periods.
         */
        private Boolean timeRestricted = false;

        /**
         * Maximum daily accesses allowed.
         */
        private Integer maxDailyAccess;

    }

}