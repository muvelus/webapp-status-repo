package com.engineerplatform.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "work_summaries")
public class WorkSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Column(name = "summary_date")
    private LocalDate summaryDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "summary_type")
    private SummaryType summaryType;
    
    @Column(name = "github_commits", columnDefinition = "TEXT")
    private String githubCommits;
    
    @Column(name = "github_prs", columnDefinition = "TEXT")
    private String githubPullRequests;
    
    @Column(name = "github_reviews", columnDefinition = "TEXT")
    private String githubReviews;
    
    @Column(name = "jira_tickets", columnDefinition = "TEXT")
    private String jiraTickets;
    
    @Column(name = "confluence_docs", columnDefinition = "TEXT")
    private String confluenceDocs;
    
    @Column(name = "slack_messages", columnDefinition = "TEXT")
    private String slackMessages;
    
    @Column(name = "meetings_attended", columnDefinition = "TEXT")
    private String meetingsAttended;
    
    @Column(name = "customer_issues_resolved", columnDefinition = "TEXT")
    private String customerIssuesResolved;
    
    @Column(name = "ai_generated_summary", columnDefinition = "TEXT")
    private String aiGeneratedSummary;
    
    @Column(name = "key_achievements", columnDefinition = "TEXT")
    private String keyAchievements;
    
    @Column(name = "collaboration_score")
    private Integer collaborationScore;
    
    @Column(name = "productivity_score")
    private Integer productivityScore;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum SummaryType {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    
    public SummaryType getSummaryType() { return summaryType; }
    public void setSummaryType(SummaryType summaryType) { this.summaryType = summaryType; }
    
    public String getGithubCommits() { return githubCommits; }
    public void setGithubCommits(String githubCommits) { this.githubCommits = githubCommits; }
    
    public String getGithubPullRequests() { return githubPullRequests; }
    public void setGithubPullRequests(String githubPullRequests) { this.githubPullRequests = githubPullRequests; }
    
    public String getGithubReviews() { return githubReviews; }
    public void setGithubReviews(String githubReviews) { this.githubReviews = githubReviews; }
    
    public String getJiraTickets() { return jiraTickets; }
    public void setJiraTickets(String jiraTickets) { this.jiraTickets = jiraTickets; }
    
    public String getConfluenceDocs() { return confluenceDocs; }
    public void setConfluenceDocs(String confluenceDocs) { this.confluenceDocs = confluenceDocs; }
    
    public String getSlackMessages() { return slackMessages; }
    public void setSlackMessages(String slackMessages) { this.slackMessages = slackMessages; }
    
    public String getMeetingsAttended() { return meetingsAttended; }
    public void setMeetingsAttended(String meetingsAttended) { this.meetingsAttended = meetingsAttended; }
    
    public String getCustomerIssuesResolved() { return customerIssuesResolved; }
    public void setCustomerIssuesResolved(String customerIssuesResolved) { this.customerIssuesResolved = customerIssuesResolved; }
    
    public String getAiGeneratedSummary() { return aiGeneratedSummary; }
    public void setAiGeneratedSummary(String aiGeneratedSummary) { this.aiGeneratedSummary = aiGeneratedSummary; }
    
    public String getKeyAchievements() { return keyAchievements; }
    public void setKeyAchievements(String keyAchievements) { this.keyAchievements = keyAchievements; }
    
    public Integer getCollaborationScore() { return collaborationScore; }
    public void setCollaborationScore(Integer collaborationScore) { this.collaborationScore = collaborationScore; }
    
    public Integer getProductivityScore() { return productivityScore; }
    public void setProductivityScore(Integer productivityScore) { this.productivityScore = productivityScore; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkSummary)) return false;
        WorkSummary that = (WorkSummary) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkSummary{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : null) +
                ", summaryDate=" + summaryDate +
                ", summaryType=" + summaryType +
                '}';
    }
}
