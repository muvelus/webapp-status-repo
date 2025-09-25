package com.engineerplatform.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkSummaryDto {
    
    private Long id;
    private Long userId;
    private String username;
    private LocalDate summaryDate;
    private String summaryType;
    private String aiGeneratedSummary;
    private String keyAchievements;
    private Integer productivityScore;
    private Integer collaborationScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public WorkSummaryDto() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    
    public String getSummaryType() { return summaryType; }
    public void setSummaryType(String summaryType) { this.summaryType = summaryType; }
    
    public String getAiGeneratedSummary() { return aiGeneratedSummary; }
    public void setAiGeneratedSummary(String aiGeneratedSummary) { this.aiGeneratedSummary = aiGeneratedSummary; }
    
    public String getKeyAchievements() { return keyAchievements; }
    public void setKeyAchievements(String keyAchievements) { this.keyAchievements = keyAchievements; }
    
    public Integer getProductivityScore() { return productivityScore; }
    public void setProductivityScore(Integer productivityScore) { this.productivityScore = productivityScore; }
    
    public Integer getCollaborationScore() { return collaborationScore; }
    public void setCollaborationScore(Integer collaborationScore) { this.collaborationScore = collaborationScore; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "WorkSummaryDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", summaryDate=" + summaryDate +
                ", summaryType='" + summaryType + '\'' +
                ", productivityScore=" + productivityScore +
                ", collaborationScore=" + collaborationScore +
                '}';
    }
}
