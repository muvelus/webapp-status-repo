package com.engineerplatform.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 100)
    @Column(unique = true)
    private String username;
    
    @NotBlank
    @Email
    @Size(max = 150)
    @Column(unique = true)
    private String email;
    
    @NotBlank
    @Size(max = 100)
    private String firstName;
    
    @NotBlank
    @Size(max = 100)
    private String lastName;
    
    @Size(max = 255)
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role = Role.ENGINEER;
    
    @Column(name = "is_enabled")
    private boolean enabled = true;
    
    @Column(name = "is_account_non_expired")
    private boolean accountNonExpired = true;
    
    @Column(name = "is_account_non_locked")
    private boolean accountNonLocked = true;
    
    @Column(name = "is_credentials_non_expired")
    private boolean credentialsNonExpired = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Size(max = 50)
    @Column(name = "github_username")
    private String githubUsername;
    
    @Size(max = 50)
    @Column(name = "slack_user_id")
    private String slackUserId;
    
    @Size(max = 50)
    @Column(name = "jira_username")
    private String jiraUsername;
    
    @Size(max = 100)
    @Column(name = "confluence_username")
    private String confluenceUsername;
    
    @Size(max = 100)
    @Column(name = "google_email")
    private String googleEmail;
    
    @Size(max = 100)
    @Column(name = "microsoft_email")
    private String microsoftEmail;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;
    
    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<User> directReports = new HashSet<>();
    
    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private Set<Team> teams = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<WorkSummary> workSummaries = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ReportSchedule> reportSchedules = new HashSet<>();
    
    public enum Role {
        ENGINEER, MANAGER, LEADER, ADMIN
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
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isManager() {
        return role == Role.MANAGER || role == Role.LEADER || role == Role.ADMIN;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public void setPassword(String password) { this.password = password; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }
    public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }
    public void setCredentialsNonExpired(boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }
    
    public String getSlackUserId() { return slackUserId; }
    public void setSlackUserId(String slackUserId) { this.slackUserId = slackUserId; }
    
    public String getJiraUsername() { return jiraUsername; }
    public void setJiraUsername(String jiraUsername) { this.jiraUsername = jiraUsername; }
    
    public String getConfluenceUsername() { return confluenceUsername; }
    public void setConfluenceUsername(String confluenceUsername) { this.confluenceUsername = confluenceUsername; }
    
    public String getGoogleEmail() { return googleEmail; }
    public void setGoogleEmail(String googleEmail) { this.googleEmail = googleEmail; }
    
    public String getMicrosoftEmail() { return microsoftEmail; }
    public void setMicrosoftEmail(String microsoftEmail) { this.microsoftEmail = microsoftEmail; }
    
    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }
    
    public Set<User> getDirectReports() { return directReports; }
    public void setDirectReports(Set<User> directReports) { this.directReports = directReports; }
    
    public Set<Team> getTeams() { return teams; }
    public void setTeams(Set<Team> teams) { this.teams = teams; }
    
    public Set<WorkSummary> getWorkSummaries() { return workSummaries; }
    public void setWorkSummaries(Set<WorkSummary> workSummaries) { this.workSummaries = workSummaries; }
    
    public Set<ReportSchedule> getReportSchedules() { return reportSchedules; }
    public void setReportSchedules(Set<ReportSchedule> reportSchedules) { this.reportSchedules = reportSchedules; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                '}';
    }
}
