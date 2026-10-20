package com.example.excelviz.model;

import java.util.List;

public class ChatRequest {
    private String message;
    private List<ChatMessage> history;
    private String activeSite;
    private Integer activeYear;

    public ChatRequest() {}

    public ChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }

    public String getActiveSite() {
        return activeSite;
    }

    public void setActiveSite(String activeSite) {
        this.activeSite = activeSite;
    }

    public Integer getActiveYear() {
        return activeYear;
    }

    public void setActiveYear(Integer activeYear) {
        this.activeYear = activeYear;
    }
}
