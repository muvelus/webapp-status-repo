package com.engineerplatform.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "report_schedules")
public class ReportSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "schedule_name")
    private String scheduleName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type")
    private ReportType reportType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency")
    private Frequency frequency;
    
    @Column(name = "delivery_time")
    private LocalTime deliveryTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_day")
    private DayOfWeek deliveryDay;
    
    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "recipient_email")
    private String recipientEmail;
    
    @Size(max = 500)
    @Column(name = "additional_recipients", columnDefinition = "TEXT")
    private String additionalRecipients;
    
    @Column(name = "include_team_summary")
    private boolean includeTeamSummary = false;
    
    @Column(name = "is_active")
    private boolean active = true;
    
    @Column(name = "last_sent")
    private LocalDateTime lastSent;
    
    @Column(name = "next_scheduled")
    private LocalDateTime nextScheduled;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ReportType {
        INDIVIDUAL_SUMMARY, TEAM_SUMMARY, MANAGER_OVERVIEW
    }
    
    public enum Frequency {
        DAILY, WEEKLY, MONTHLY
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
    
    public String getScheduleName() { return scheduleName; }
    public void setScheduleName(String scheduleName) { this.scheduleName = scheduleName; }
    
    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }
    
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    
    public LocalTime getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(LocalTime deliveryTime) { this.deliveryTime = deliveryTime; }
    
    public DayOfWeek getDeliveryDay() { return deliveryDay; }
    public void setDeliveryDay(DayOfWeek deliveryDay) { this.deliveryDay = deliveryDay; }
    
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    
    public String getAdditionalRecipients() { return additionalRecipients; }
    public void setAdditionalRecipients(String additionalRecipients) { this.additionalRecipients = additionalRecipients; }
    
    public boolean isIncludeTeamSummary() { return includeTeamSummary; }
    public void setIncludeTeamSummary(boolean includeTeamSummary) { this.includeTeamSummary = includeTeamSummary; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getLastSent() { return lastSent; }
    public void setLastSent(LocalDateTime lastSent) { this.lastSent = lastSent; }
    
    public LocalDateTime getNextScheduled() { return nextScheduled; }
    public void setNextScheduled(LocalDateTime nextScheduled) { this.nextScheduled = nextScheduled; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportSchedule)) return false;
        ReportSchedule that = (ReportSchedule) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ReportSchedule{" +
                "id=" + id +
                ", scheduleName='" + scheduleName + '\'' +
                ", reportType=" + reportType +
                ", frequency=" + frequency +
                ", active=" + active +
                '}';
    }
}
