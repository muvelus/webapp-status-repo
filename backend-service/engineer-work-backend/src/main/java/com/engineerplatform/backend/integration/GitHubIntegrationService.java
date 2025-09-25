package com.engineerplatform.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitHubIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubIntegrationService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.integrations.github.token}")
    private String githubToken;
    
    public GitHubIntegrationService(@Value("${app.integrations.github.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Engineer-Work-Platform/1.0")
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<GitHubCommit> getUserCommits(String username, LocalDateTime since, LocalDateTime until) {
        logger.info("Fetching GitHub commits for user: {} from {} to {}", username, since, until);
        
        List<GitHubCommit> commits = new ArrayList<>();
        
        try {
            String sinceParam = since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            String untilParam = until.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/commits")
                            .queryParam("q", "author:" + username + " author-date:" + sinceParam + ".." + untilParam)
                            .queryParam("sort", "author-date")
                            .queryParam("order", "desc")
                            .queryParam("per_page", "100")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                    .header("Accept", "application/vnd.github.cloak-preview")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode items = jsonResponse.get("items");
            
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    GitHubCommit commit = parseCommit(item);
                    if (commit != null) {
                        commits.add(commit);
                    }
                }
            }
            
            logger.info("Successfully fetched {} commits for user: {}", commits.size(), username);
            
        } catch (WebClientResponseException e) {
            logger.error("Error fetching GitHub commits for user {}: {} - {}", username, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching GitHub commits for user {}: {}", username, e.getMessage(), e);
        }
        
        return commits;
    }
    
    public List<GitHubPullRequest> getUserPullRequests(String username, LocalDateTime since, LocalDateTime until) {
        logger.info("Fetching GitHub pull requests for user: {} from {} to {}", username, since, until);
        
        List<GitHubPullRequest> pullRequests = new ArrayList<>();
        
        try {
            String sinceParam = since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            String untilParam = until.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/issues")
                            .queryParam("q", "type:pr author:" + username + " created:" + sinceParam + ".." + untilParam)
                            .queryParam("sort", "created")
                            .queryParam("order", "desc")
                            .queryParam("per_page", "100")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode items = jsonResponse.get("items");
            
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    GitHubPullRequest pr = parsePullRequest(item);
                    if (pr != null) {
                        pullRequests.add(pr);
                    }
                }
            }
            
            logger.info("Successfully fetched {} pull requests for user: {}", pullRequests.size(), username);
            
        } catch (WebClientResponseException e) {
            logger.error("Error fetching GitHub pull requests for user {}: {} - {}", username, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching GitHub pull requests for user {}: {}", username, e.getMessage(), e);
        }
        
        return pullRequests;
    }
    
    public List<GitHubReview> getUserReviews(String username, LocalDateTime since, LocalDateTime until) {
        logger.info("Fetching GitHub reviews for user: {} from {} to {}", username, since, until);
        
        List<GitHubReview> reviews = new ArrayList<>();
        
        try {
            String sinceParam = since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            String untilParam = until.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/issues")
                            .queryParam("q", "type:pr reviewed-by:" + username + " updated:" + sinceParam + ".." + untilParam)
                            .queryParam("sort", "updated")
                            .queryParam("order", "desc")
                            .queryParam("per_page", "100")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode items = jsonResponse.get("items");
            
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    GitHubReview review = parseReview(item, username);
                    if (review != null) {
                        reviews.add(review);
                    }
                }
            }
            
            logger.info("Successfully fetched {} reviews for user: {}", reviews.size(), username);
            
        } catch (WebClientResponseException e) {
            logger.error("Error fetching GitHub reviews for user {}: {} - {}", username, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching GitHub reviews for user {}: {}", username, e.getMessage(), e);
        }
        
        return reviews;
    }
    
    private GitHubCommit parseCommit(JsonNode commitNode) {
        try {
            GitHubCommit commit = new GitHubCommit();
            commit.setSha(commitNode.get("sha").asText());
            commit.setMessage(commitNode.path("commit").path("message").asText());
            commit.setAuthor(commitNode.path("commit").path("author").path("name").asText());
            commit.setDate(commitNode.path("commit").path("author").path("date").asText());
            commit.setRepository(commitNode.path("repository").path("full_name").asText());
            commit.setUrl(commitNode.get("html_url").asText());
            return commit;
        } catch (Exception e) {
            logger.error("Error parsing GitHub commit: {}", e.getMessage());
            return null;
        }
    }
    
    private GitHubPullRequest parsePullRequest(JsonNode prNode) {
        try {
            GitHubPullRequest pr = new GitHubPullRequest();
            pr.setNumber(prNode.get("number").asInt());
            pr.setTitle(prNode.get("title").asText());
            pr.setState(prNode.get("state").asText());
            pr.setCreatedAt(prNode.get("created_at").asText());
            pr.setUpdatedAt(prNode.get("updated_at").asText());
            pr.setRepository(prNode.path("repository_url").asText());
            pr.setUrl(prNode.get("html_url").asText());
            return pr;
        } catch (Exception e) {
            logger.error("Error parsing GitHub pull request: {}", e.getMessage());
            return null;
        }
    }
    
    private GitHubReview parseReview(JsonNode reviewNode, String username) {
        try {
            GitHubReview review = new GitHubReview();
            review.setPullRequestNumber(reviewNode.get("number").asInt());
            review.setPullRequestTitle(reviewNode.get("title").asText());
            review.setReviewer(username);
            review.setUpdatedAt(reviewNode.get("updated_at").asText());
            review.setRepository(reviewNode.path("repository_url").asText());
            review.setUrl(reviewNode.get("html_url").asText());
            return review;
        } catch (Exception e) {
            logger.error("Error parsing GitHub review: {}", e.getMessage());
            return null;
        }
    }
    
    public static class GitHubCommit {
        private String sha;
        private String message;
        private String author;
        private String date;
        private String repository;
        private String url;
        
        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getRepository() { return repository; }
        public void setRepository(String repository) { this.repository = repository; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
    
    public static class GitHubPullRequest {
        private int number;
        private String title;
        private String state;
        private String createdAt;
        private String updatedAt;
        private String repository;
        private String url;
        
        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        
        public String getRepository() { return repository; }
        public void setRepository(String repository) { this.repository = repository; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
    
    public static class GitHubReview {
        private int pullRequestNumber;
        private String pullRequestTitle;
        private String reviewer;
        private String updatedAt;
        private String repository;
        private String url;
        
        public int getPullRequestNumber() { return pullRequestNumber; }
        public void setPullRequestNumber(int pullRequestNumber) { this.pullRequestNumber = pullRequestNumber; }
        
        public String getPullRequestTitle() { return pullRequestTitle; }
        public void setPullRequestTitle(String pullRequestTitle) { this.pullRequestTitle = pullRequestTitle; }
        
        public String getReviewer() { return reviewer; }
        public void setReviewer(String reviewer) { this.reviewer = reviewer; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        
        public String getRepository() { return repository; }
        public void setRepository(String repository) { this.repository = repository; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
