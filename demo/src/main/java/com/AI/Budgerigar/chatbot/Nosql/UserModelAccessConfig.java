package com.AI.Budgerigar.chatbot.Nosql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * UserModelAccessConfig class represents the configuration for user model access.
 *
 * <p>
 * It contains the following fields:
 * <ul>
 * <li>userId: the user ID</li>
 * <li>allowedModels: list of allowed model accesses</li>
 * <li>version: version number of the access control configuration</li>
 * <li>maxDailyAccesses: maximum daily accesses allowed</li>
 * <li>expirationDate: expiration date of the configuration (optional)</li>
 * <li>disabled: indicates whether the configuration is disabled</li>
 * <li>metadata: additional metadata for future extensions</li>
 * </ul>
 * It also contains the following nested classes:
 * <ul>
 * <li>ModelAccess: class representing the access details for a specific model</li>
 * <li>AccessRestriction: class representing the restrictions on model access</li>
 * </ul>
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
     *
     * <p>
     * It contains the following fields:
     * <ul>
     * <li>url: URL of the model</li>
     * <li>model: name of the model</li>
     * <li>accessLevel: access level of the model (e.g., read-only, full access)</li>
     * <li>accessRestriction: additional access restrictions for the model</li>
     * <li>priority: priority of the model access</li>
     * <li>additionalAttributes: additional attributes for future use</li>
     * </ul>
     * </p>
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor // 添加无参构造函数
    public static class ModelAccess {

        public ModelAccess(String url, String model) {
            this.url = url;
            this.model = model;
        }

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
        private String accessLevel = "full";

        /**
         * Additional access restrictions for the model.
         */
        private AccessRestriction accessRestriction = null;

        /**
         * Priority of the model access.
         */
        private Integer priority = 0;

        /**
         * Additional attributes for future use.
         */
        private Map<String, Object> additionalAttributes = null;

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