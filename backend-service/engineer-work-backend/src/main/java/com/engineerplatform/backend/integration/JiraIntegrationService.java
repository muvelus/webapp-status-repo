package com.engineerplatform.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class JiraIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(JiraIntegrationService.class);
    
    @Value("${app.integrations.jira.base-url:}")
    private String jiraBaseUrl;
    
    @Value("${app.integrations.jira.username:}")
    private String jiraUsername;
    
    @Value("${app.integrations.jira.api-token:}")
    private String jiraApiToken;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public JiraIntegrationService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<JiraTicket> getUserTickets(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Jira tickets for user {} from {} to {}", userEmail, startTime, endTime);
        
        if (jiraBaseUrl == null || jiraBaseUrl.isEmpty() || 
            jiraUsername == null || jiraUsername.isEmpty() || 
            jiraApiToken == null || jiraApiToken.isEmpty()) {
            logger.warn("Jira configuration not complete, skipping ticket fetch");
            return new ArrayList<>();
        }
        
        try {
            String jql = buildJqlQuery(userEmail, startTime, endTime);
            String response = executeJqlQuery(jql);
            
            List<JiraTicket> tickets = parseTicketsFromResponse(response);
            logger.info("Retrieved {} Jira tickets for user {}", tickets.size(), userEmail);
            return tickets;
            
        } catch (Exception e) {
            logger.error("Error fetching Jira tickets for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<JiraTicket> getTicketUpdates(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Jira ticket updates for user {} from {} to {}", userEmail, startTime, endTime);
        
        try {
            String jql = buildUpdateJqlQuery(userEmail, startTime, endTime);
            String response = executeJqlQuery(jql);
            
            List<JiraTicket> tickets = parseTicketsFromResponse(response);
            logger.info("Retrieved {} Jira ticket updates for user {}", tickets.size(), userEmail);
            return tickets;
            
        } catch (Exception e) {
            logger.error("Error fetching Jira ticket updates for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<JiraComment> getUserComments(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Jira comments for user {} from {} to {}", userEmail, startTime, endTime);
        
        try {
            List<JiraTicket> tickets = getUserTickets(userEmail, startTime, endTime);
            List<JiraComment> allComments = new ArrayList<>();
            
            for (JiraTicket ticket : tickets) {
                List<JiraComment> ticketComments = getTicketComments(ticket.getKey(), userEmail, startTime, endTime);
                allComments.addAll(ticketComments);
            }
            
            logger.info("Retrieved {} Jira comments for user {}", allComments.size(), userEmail);
            return allComments;
            
        } catch (Exception e) {
            logger.error("Error fetching Jira comments for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private String buildJqlQuery(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        return String.format(
            "assignee = '%s' AND created >= '%s' AND created <= '%s' ORDER BY created DESC",
            userEmail,
            startTime.format(formatter),
            endTime.format(formatter)
        );
    }
    
    private String buildUpdateJqlQuery(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        return String.format(
            "assignee = '%s' AND updated >= '%s' AND updated <= '%s' ORDER BY updated DESC",
            userEmail,
            startTime.format(formatter),
            endTime.format(formatter)
        );
    }
    
    private String executeJqlQuery(String jql) {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
            (jiraUsername + ":" + jiraApiToken).getBytes()
        );
        
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(jiraBaseUrl + "/rest/api/3/search")
                .queryParam("jql", jql)
                .queryParam("maxResults", 100)
                .queryParam("fields", "key,summary,status,assignee,created,updated,description,priority")
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .header("Content-Type", "application/json")
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
    
    private List<JiraTicket> parseTicketsFromResponse(String response) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(response);
        List<JiraTicket> tickets = new ArrayList<>();
        
        if (jsonResponse.has("issues")) {
            JsonNode issuesNode = jsonResponse.get("issues");
            for (JsonNode issueNode : issuesNode) {
                JiraTicket ticket = new JiraTicket();
                ticket.setKey(issueNode.get("key").asText());
                
                JsonNode fieldsNode = issueNode.get("fields");
                ticket.setSummary(fieldsNode.get("summary").asText());
                ticket.setStatus(fieldsNode.get("status").get("name").asText());
                
                if (fieldsNode.has("description") && !fieldsNode.get("description").isNull()) {
                    ticket.setDescription(fieldsNode.get("description").asText());
                }
                
                if (fieldsNode.has("priority") && !fieldsNode.get("priority").isNull()) {
                    ticket.setPriority(fieldsNode.get("priority").get("name").asText());
                }
                
                if (fieldsNode.has("assignee") && !fieldsNode.get("assignee").isNull()) {
                    ticket.setAssignee(fieldsNode.get("assignee").get("emailAddress").asText());
                }
                
                ticket.setCreated(LocalDateTime.parse(
                    fieldsNode.get("created").asText().substring(0, 19)
                ));
                ticket.setUpdated(LocalDateTime.parse(
                    fieldsNode.get("updated").asText().substring(0, 19)
                ));
                
                tickets.add(ticket);
            }
        }
        
        return tickets;
    }
    
    private List<JiraComment> getTicketComments(String ticketKey, String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching comments for ticket {} by user {}", ticketKey, userEmail);
        
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (jiraUsername + ":" + jiraApiToken).getBytes()
            );
            
            String response = webClient.get()
                .uri(jiraBaseUrl + "/rest/api/3/issue/" + ticketKey + "/comment")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<JiraComment> comments = new ArrayList<>();
            
            if (jsonResponse.has("comments")) {
                JsonNode commentsNode = jsonResponse.get("comments");
                for (JsonNode commentNode : commentsNode) {
                    String authorEmail = commentNode.get("author").get("emailAddress").asText();
                    if (authorEmail.equals(userEmail)) {
                        LocalDateTime commentTime = LocalDateTime.parse(
                            commentNode.get("created").asText().substring(0, 19)
                        );
                        
                        if (commentTime.isAfter(startTime) && commentTime.isBefore(endTime)) {
                            JiraComment comment = new JiraComment();
                            comment.setTicketKey(ticketKey);
                            comment.setAuthor(authorEmail);
                            comment.setBody(commentNode.get("body").asText());
                            comment.setCreated(commentTime);
                            comments.add(comment);
                        }
                    }
                }
            }
            
            return comments;
            
        } catch (Exception e) {
            logger.error("Error fetching comments for ticket {}: {}", ticketKey, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public boolean testConnection() {
        logger.debug("Testing Jira API connection");
        
        if (jiraBaseUrl == null || jiraBaseUrl.isEmpty() || 
            jiraUsername == null || jiraUsername.isEmpty() || 
            jiraApiToken == null || jiraApiToken.isEmpty()) {
            logger.warn("Jira configuration not complete");
            return false;
        }
        
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (jiraUsername + ":" + jiraApiToken).getBytes()
            );
            
            String response = webClient.get()
                .uri(jiraBaseUrl + "/rest/api/3/myself")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            boolean isValid = jsonResponse.has("accountId");
            
            if (isValid) {
                logger.info("Jira API connection successful");
            } else {
                logger.error("Jira API connection failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error testing Jira API connection: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public static class JiraTicket {
        private String key;
        private String summary;
        private String description;
        private String status;
        private String priority;
        private String assignee;
        private LocalDateTime created;
        private LocalDateTime updated;
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        
        public LocalDateTime getCreated() { return created; }
        public void setCreated(LocalDateTime created) { this.created = created; }
        
        public LocalDateTime getUpdated() { return updated; }
        public void setUpdated(LocalDateTime updated) { this.updated = updated; }
        
        @Override
        public String toString() {
            return "JiraTicket{" +
                    "key='" + key + '\'' +
                    ", summary='" + summary + '\'' +
                    ", status='" + status + '\'' +
                    ", assignee='" + assignee + '\'' +
                    ", created=" + created +
                    ", updated=" + updated +
                    '}';
        }
    }
    
    public static class JiraComment {
        private String ticketKey;
        private String author;
        private String body;
        private LocalDateTime created;
        
        public String getTicketKey() { return ticketKey; }
        public void setTicketKey(String ticketKey) { this.ticketKey = ticketKey; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        public LocalDateTime getCreated() { return created; }
        public void setCreated(LocalDateTime created) { this.created = created; }
        
        @Override
        public String toString() {
            return "JiraComment{" +
                    "ticketKey='" + ticketKey + '\'' +
                    ", author='" + author + '\'' +
                    ", body='" + body + '\'' +
                    ", created=" + created +
                    '}';
        }
    }
}
