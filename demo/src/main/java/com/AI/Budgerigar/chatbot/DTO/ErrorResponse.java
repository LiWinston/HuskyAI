package com.AI.Budgerigar.chatbot.DTO;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Getter
public class ErrorResponse {
    private static final Logger logger = LoggerFactory.getLogger(ErrorResponse.class);


    // Getter and Setter
    private String error;

    public ErrorResponse(String error) {
        this.error = error;
        logger.error(error);
    }

    public void setError(String error) {
        this.error = error;
    }
}
