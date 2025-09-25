package com.engineerplatform.backend.service;

import com.engineerplatform.backend.dto.WorkSummaryDto;
import com.engineerplatform.backend.exception.ResourceNotFoundException;
import com.engineerplatform.backend.integration.GitHubIntegrationService;
import com.engineerplatform.backend.integration.OllamaIntegrationService;
import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.model.WorkSummary;
import com.engineerplatform.backend.repository.WorkSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkSummaryService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkSummaryService.class);
    
    private final WorkSummaryRepository workSummaryRepository;
    private final UserService userService;
    private final GitHubIntegrationService gitHubService;
    private final OllamaIntegrationService ollamaService;
    
    @Autowired
    public WorkSummaryService(WorkSummaryRepository workSummaryRepository,
                             UserService userService,
                             GitHubIntegrationService gitHubService,
                             OllamaIntegrationService ollamaService) {
        this.workSummaryRepository = workSummaryRepository;
        this.userService = userService;
        this.gitHubService = gitHubService;
        this.ollamaService = ollamaService;
    }
    
    public WorkSummary generateDailySummary(Long userId, LocalDate date) {
        logger.info("Generating daily summary for user {} on date {}", userId, date);
        
        User user = userService.getUserById(userId);
        
        Optional<WorkSummary> existingSummary = workSummaryRepository
            .findByUserAndSummaryDateAndSummaryType(user, date, WorkSummary.SummaryType.DAILY);
        
        if (existingSummary.isPresent()) {
            logger.info("Daily summary already exists for user {} on date {}", userId, date);
            return existingSummary.get();
        }
        
        WorkSummary summary = new WorkSummary();
        summary.setUser(user);
        summary.setSummaryDate(date);
        summary.setSummaryType(WorkSummary.SummaryType.DAILY);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        try {
            if (user.getGithubUsername() != null) {
                var commits = gitHubService.getUserCommits(user.getGithubUsername(), startOfDay, endOfDay);
                var pullRequests = gitHubService.getUserPullRequests(user.getGithubUsername(), startOfDay, endOfDay);
                var reviews = gitHubService.getUserReviews(user.getGithubUsername(), startOfDay, endOfDay);
                
                summary.setGithubCommits(formatGitHubCommits(commits));
                summary.setGithubPullRequests(formatGitHubPullRequests(pullRequests));
                summary.setGithubReviews(formatGitHubReviews(reviews));
            }
            
            String workData = buildWorkDataString(summary);
            String aiSummary = ollamaService.generateWorkSummary(workData);
            summary.setAiGeneratedSummary(aiSummary);
            
            summary.setProductivityScore(calculateProductivityScore(summary));
            summary.setCollaborationScore(calculateCollaborationScore(summary));
            
            WorkSummary savedSummary = workSummaryRepository.save(summary);
            logger.info("Successfully generated daily summary for user {} on date {}", userId, date);
            return savedSummary;
            
        } catch (Exception e) {
            logger.error("Error generating daily summary for user {} on date {}: {}", userId, date, e.getMessage(), e);
            throw new RuntimeException("Failed to generate daily summary", e);
        }
    }
    
    public WorkSummary generateWeeklySummary(Long userId, LocalDate weekStartDate) {
        logger.info("Generating weekly summary for user {} starting from {}", userId, weekStartDate);
        
        User user = userService.getUserById(userId);
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        
        Optional<WorkSummary> existingSummary = workSummaryRepository
            .findByUserAndSummaryDateAndSummaryType(user, weekStartDate, WorkSummary.SummaryType.WEEKLY);
        
        if (existingSummary.isPresent()) {
            logger.info("Weekly summary already exists for user {} for week starting {}", userId, weekStartDate);
            return existingSummary.get();
        }
        
        List<WorkSummary> dailySummaries = workSummaryRepository
            .findByUserAndDateRange(user, weekStartDate, weekEndDate);
        
        WorkSummary weeklySummary = aggregateWeeklySummary(user, weekStartDate, dailySummaries);
        
        WorkSummary savedSummary = workSummaryRepository.save(weeklySummary);
        logger.info("Successfully generated weekly summary for user {} for week starting {}", userId, weekStartDate);
        return savedSummary;
    }
    
    public List<WorkSummary> getUserSummaries(Long userId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching work summaries for user {} from {} to {}", userId, startDate, endDate);
        
        User user = userService.getUserById(userId);
        return workSummaryRepository.findByUserAndDateRange(user, startDate, endDate);
    }
    
    public List<WorkSummary> getTeamSummaries(Long managerId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching team summaries for manager {} from {} to {}", managerId, startDate, endDate);
        
        User manager = userService.getUserById(managerId);
        List<User> teamMembers = userService.getDirectReports(manager);
        
        return workSummaryRepository.findByUsersAndDateRange(teamMembers, startDate, endDate);
    }
    
    public WorkSummaryDto convertToDto(WorkSummary summary) {
        WorkSummaryDto dto = new WorkSummaryDto();
        dto.setId(summary.getId());
        dto.setUserId(summary.getUser().getId());
        dto.setUsername(summary.getUser().getUsername());
        dto.setSummaryDate(summary.getSummaryDate());
        dto.setSummaryType(summary.getSummaryType().name());
        dto.setAiGeneratedSummary(summary.getAiGeneratedSummary());
        dto.setKeyAchievements(summary.getKeyAchievements());
        dto.setProductivityScore(summary.getProductivityScore());
        dto.setCollaborationScore(summary.getCollaborationScore());
        dto.setCreatedAt(summary.getCreatedAt());
        dto.setUpdatedAt(summary.getUpdatedAt());
        return dto;
    }
    
    public List<WorkSummaryDto> convertToDtoList(List<WorkSummary> summaries) {
        return summaries.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public WorkSummary getSummaryById(Long summaryId) {
        logger.debug("Fetching work summary by ID: {}", summaryId);
        
        Optional<WorkSummary> summary = workSummaryRepository.findById(summaryId);
        if (summary.isEmpty()) {
            throw new ResourceNotFoundException("Work summary not found with ID: " + summaryId);
        }
        
        return summary.get();
    }
    
    private String formatGitHubCommits(List<GitHubIntegrationService.GitHubCommit> commits) {
        if (commits.isEmpty()) return "No commits";
        
        StringBuilder sb = new StringBuilder();
        for (GitHubIntegrationService.GitHubCommit commit : commits) {
            sb.append("- ").append(commit.getMessage()).append(" (").append(commit.getRepository()).append(")\n");
        }
        return sb.toString();
    }
    
    private String formatGitHubPullRequests(List<GitHubIntegrationService.GitHubPullRequest> prs) {
        if (prs.isEmpty()) return "No pull requests";
        
        StringBuilder sb = new StringBuilder();
        for (GitHubIntegrationService.GitHubPullRequest pr : prs) {
            sb.append("- ").append(pr.getTitle()).append(" (#").append(pr.getNumber()).append(")\n");
        }
        return sb.toString();
    }
    
    private String formatGitHubReviews(List<GitHubIntegrationService.GitHubReview> reviews) {
        if (reviews.isEmpty()) return "No reviews";
        
        StringBuilder sb = new StringBuilder();
        for (GitHubIntegrationService.GitHubReview review : reviews) {
            sb.append("- Reviewed: ").append(review.getPullRequestTitle()).append("\n");
        }
        return sb.toString();
    }
    
    private String buildWorkDataString(WorkSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("GitHub Commits:\n").append(summary.getGithubCommits()).append("\n\n");
        sb.append("GitHub Pull Requests:\n").append(summary.getGithubPullRequests()).append("\n\n");
        sb.append("GitHub Reviews:\n").append(summary.getGithubReviews()).append("\n\n");
        
        if (summary.getJiraTickets() != null) {
            sb.append("Jira Tickets:\n").append(summary.getJiraTickets()).append("\n\n");
        }
        if (summary.getSlackMessages() != null) {
            sb.append("Slack Activity:\n").append(summary.getSlackMessages()).append("\n\n");
        }
        if (summary.getMeetingsAttended() != null) {
            sb.append("Meetings:\n").append(summary.getMeetingsAttended()).append("\n\n");
        }
        
        return sb.toString();
    }
    
    private Integer calculateProductivityScore(WorkSummary summary) {
        int score = 0;
        
        if (summary.getGithubCommits() != null && !summary.getGithubCommits().equals("No commits")) {
            score += 30;
        }
        if (summary.getGithubPullRequests() != null && !summary.getGithubPullRequests().equals("No pull requests")) {
            score += 25;
        }
        if (summary.getJiraTickets() != null && !summary.getJiraTickets().isEmpty()) {
            score += 20;
        }
        if (summary.getConfluenceDocs() != null && !summary.getConfluenceDocs().isEmpty()) {
            score += 15;
        }
        if (summary.getCustomerIssuesResolved() != null && !summary.getCustomerIssuesResolved().isEmpty()) {
            score += 10;
        }
        
        return Math.min(score, 100);
    }
    
    private Integer calculateCollaborationScore(WorkSummary summary) {
        int score = 0;
        
        if (summary.getGithubReviews() != null && !summary.getGithubReviews().equals("No reviews")) {
            score += 30;
        }
        if (summary.getSlackMessages() != null && !summary.getSlackMessages().isEmpty()) {
            score += 25;
        }
        if (summary.getMeetingsAttended() != null && !summary.getMeetingsAttended().isEmpty()) {
            score += 25;
        }
        if (summary.getCustomerIssuesResolved() != null && !summary.getCustomerIssuesResolved().isEmpty()) {
            score += 20;
        }
        
        return Math.min(score, 100);
    }
    
    private WorkSummary aggregateWeeklySummary(User user, LocalDate weekStartDate, List<WorkSummary> dailySummaries) {
        WorkSummary weeklySummary = new WorkSummary();
        weeklySummary.setUser(user);
        weeklySummary.setSummaryDate(weekStartDate);
        weeklySummary.setSummaryType(WorkSummary.SummaryType.WEEKLY);
        
        StringBuilder commits = new StringBuilder();
        StringBuilder prs = new StringBuilder();
        StringBuilder reviews = new StringBuilder();
        StringBuilder jiraTickets = new StringBuilder();
        StringBuilder meetings = new StringBuilder();
        
        int totalProductivityScore = 0;
        int totalCollaborationScore = 0;
        int summaryCount = 0;
        
        for (WorkSummary daily : dailySummaries) {
            if (daily.getGithubCommits() != null) commits.append(daily.getGithubCommits()).append("\n");
            if (daily.getGithubPullRequests() != null) prs.append(daily.getGithubPullRequests()).append("\n");
            if (daily.getGithubReviews() != null) reviews.append(daily.getGithubReviews()).append("\n");
            if (daily.getJiraTickets() != null) jiraTickets.append(daily.getJiraTickets()).append("\n");
            if (daily.getMeetingsAttended() != null) meetings.append(daily.getMeetingsAttended()).append("\n");
            
            if (daily.getProductivityScore() != null) {
                totalProductivityScore += daily.getProductivityScore();
                summaryCount++;
            }
            if (daily.getCollaborationScore() != null) {
                totalCollaborationScore += daily.getCollaborationScore();
            }
        }
        
        weeklySummary.setGithubCommits(commits.toString());
        weeklySummary.setGithubPullRequests(prs.toString());
        weeklySummary.setGithubReviews(reviews.toString());
        weeklySummary.setJiraTickets(jiraTickets.toString());
        weeklySummary.setMeetingsAttended(meetings.toString());
        
        if (summaryCount > 0) {
            weeklySummary.setProductivityScore(totalProductivityScore / summaryCount);
            weeklySummary.setCollaborationScore(totalCollaborationScore / summaryCount);
        }
        
        String weeklyData = buildWorkDataString(weeklySummary);
        String aiSummary = ollamaService.generateWorkSummary(weeklyData);
        weeklySummary.setAiGeneratedSummary(aiSummary);
        
        return weeklySummary;
    }
}
