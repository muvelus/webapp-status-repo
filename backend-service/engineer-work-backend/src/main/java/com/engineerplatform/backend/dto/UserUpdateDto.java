package com.engineerplatform.backend.dto;

import com.engineerplatform.backend.model.User;
import jakarta.validation.constraints.Size;

public class UserUpdateDto {
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @Size(max = 50, message = "GitHub username must not exceed 50 characters")
    private String githubUsername;
    
    @Size(max = 50, message = "Slack user ID must not exceed 50 characters")
    private String slackUserId;
    
    @Size(max = 50, message = "Jira username must not exceed 50 characters")
    private String jiraUsername;
    
    @Size(max = 100, message = "Confluence username must not exceed 100 characters")
    private String confluenceUsername;
    
    @Size(max = 100, message = "Google email must not exceed 100 characters")
    private String googleEmail;
    
    @Size(max = 100, message = "Microsoft email must not exceed 100 characters")
    private String microsoftEmail;
    
    private User.Role role;
    
    public UserUpdateDto() {}
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
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
    
    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }
    
    @Override
    public String toString() {
        return "UserUpdateDto{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", githubUsername='" + githubUsername + '\'' +
                ", slackUserId='" + slackUserId + '\'' +
                ", jiraUsername='" + jiraUsername + '\'' +
                ", confluenceUsername='" + confluenceUsername + '\'' +
                ", googleEmail='" + googleEmail + '\'' +
                ", microsoftEmail='" + microsoftEmail + '\'' +
                ", role=" + role +
                '}';
    }
}
