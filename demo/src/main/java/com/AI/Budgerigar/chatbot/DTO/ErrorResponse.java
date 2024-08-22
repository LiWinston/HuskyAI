package com.AI.Budgerigar.chatbot.DTO;

public class ErrorResponse {

    private String error;

    public ErrorResponse(String error) {
        this.error = error;
        System.out.println("ErrorResponse: " + error);
    }

    // Getter and Setter
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
