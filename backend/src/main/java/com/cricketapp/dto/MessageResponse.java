package com.cricketapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

    private String message;
    private String userId;

    public MessageResponse() {
    }

    public MessageResponse(String message) {
        this.message = message;
    }

    public MessageResponse(String message, String userId) {
        this.message = message;
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
