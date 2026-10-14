package com.jobpilot.dto.chat;

import java.time.LocalDateTime;

import com.jobpilot.chat.ChatMessage;

public class ChatMessageResponse {

    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(ChatMessage message) {
        this.id = message.getId();
        this.role = message.getRole();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }

    public ChatMessageResponse(Long id, String role, String content, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
