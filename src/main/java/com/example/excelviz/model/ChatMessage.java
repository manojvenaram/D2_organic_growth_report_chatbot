package com.example.excelviz.model;

import java.time.Instant;

public class ChatMessage {
    private String sender; // "user" or "bot"
    private String text;
    private Instant timestamp;
    private String actionType;
    private String actionPayload;

    public ChatMessage() {
        this.timestamp = Instant.now();
    }

    public ChatMessage(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.timestamp = Instant.now();
    }

    public ChatMessage(String sender, String text, String actionType, String actionPayload) {
        this.sender = sender;
        this.text = text;
        this.actionType = actionType;
        this.actionPayload = actionPayload;
        this.timestamp = Instant.now();
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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
}
