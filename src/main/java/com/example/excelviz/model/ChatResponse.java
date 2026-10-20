package com.example.excelviz.model;

import java.util.List;
import java.util.Map;

public class ChatResponse {
    private String reply;
    private List<String> quickSuggestions;
    private String actionType; // "SWITCH_TAB", "SELECT_SITE", "TRIGGER_COMPARE"
    private String actionPayload;
    private Map<String, Object> dataSnapshot;

    public ChatResponse() {}

    public ChatResponse(String reply) {
        this.reply = reply;
    }

    public ChatResponse(String reply, List<String> quickSuggestions) {
        this.reply = reply;
        this.quickSuggestions = quickSuggestions;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getQuickSuggestions() {
        return quickSuggestions;
    }

    public void setQuickSuggestions(List<String> quickSuggestions) {
        this.quickSuggestions = quickSuggestions;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionPayload() {
        return actionPayload;
    }

    public void setActionPayload(String actionPayload) {
        this.actionPayload = actionPayload;
    }

    public Map<String, Object> getDataSnapshot() {
        return dataSnapshot;
    }

    public void setDataSnapshot(Map<String, Object> dataSnapshot) {
        this.dataSnapshot = dataSnapshot;
    }
}
