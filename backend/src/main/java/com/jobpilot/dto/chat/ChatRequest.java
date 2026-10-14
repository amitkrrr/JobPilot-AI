package com.jobpilot.dto.chat;

import com.jobpilot.chat.AIChatClient;

public class ChatRequest {

    private String message;
    private int page = 0;
    private AIChatClient.SearchIntent lastIntent;

    public ChatRequest() {
    }

    public ChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public AIChatClient.SearchIntent getLastIntent() {
        return lastIntent;
    }

    public void setLastIntent(AIChatClient.SearchIntent lastIntent) {
        this.lastIntent = lastIntent;
    }
}
