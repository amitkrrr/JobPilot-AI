package com.jobpilot.dto.resume;

import java.time.LocalDateTime;

public class ResumeResponse {

    private Long id;
    private String fileName;
    private String contentType;
    private LocalDateTime createdAt;

    public ResumeResponse() {
    }

    public ResumeResponse(Long id, String fileName, String contentType, LocalDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
