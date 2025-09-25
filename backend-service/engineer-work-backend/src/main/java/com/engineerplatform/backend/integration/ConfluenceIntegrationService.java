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
public class ConfluenceIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceIntegrationService.class);
    
    @Value("${app.integrations.confluence.base-url:}")
    private String confluenceBaseUrl;
    
    @Value("${app.integrations.confluence.username:}")
    private String confluenceUsername;
    
    @Value("${app.integrations.confluence.api-token:}")
    private String confluenceApiToken;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public ConfluenceIntegrationService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<ConfluencePage> getUserPages(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Confluence pages for user {} from {} to {}", userEmail, startTime, endTime);
        
        if (!isConfigured()) {
            logger.warn("Confluence configuration not complete, skipping page fetch");
            return new ArrayList<>();
        }
        
        try {
            String cql = buildCqlQuery(userEmail, startTime, endTime, "created");
            String response = executeCqlQuery(cql);
            
            List<ConfluencePage> pages = parsePagesFromResponse(response);
            logger.info("Retrieved {} Confluence pages for user {}", pages.size(), userEmail);
            return pages;
            
        } catch (Exception e) {
            logger.error("Error fetching Confluence pages for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<ConfluencePage> getUserPageUpdates(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Confluence page updates for user {} from {} to {}", userEmail, startTime, endTime);
        
        try {
            String cql = buildCqlQuery(userEmail, startTime, endTime, "lastModified");
            String response = executeCqlQuery(cql);
            
            List<ConfluencePage> pages = parsePagesFromResponse(response);
            logger.info("Retrieved {} Confluence page updates for user {}", pages.size(), userEmail);
            return pages;
            
        } catch (Exception e) {
            logger.error("Error fetching Confluence page updates for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<ConfluenceComment> getUserComments(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Confluence comments for user {} from {} to {}", userEmail, startTime, endTime);
        
        try {
            List<ConfluencePage> pages = getUserPages(userEmail, startTime, endTime);
            List<ConfluenceComment> allComments = new ArrayList<>();
            
            for (ConfluencePage page : pages) {
                List<ConfluenceComment> pageComments = getPageComments(page.getId(), userEmail, startTime, endTime);
                allComments.addAll(pageComments);
            }
            
            logger.info("Retrieved {} Confluence comments for user {}", allComments.size(), userEmail);
            return allComments;
            
        } catch (Exception e) {
            logger.error("Error fetching Confluence comments for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<ConfluenceSpace> getUserSpaces(String userEmail) {
        logger.debug("Fetching Confluence spaces for user {}", userEmail);
        
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (confluenceUsername + ":" + confluenceApiToken).getBytes()
            );
            
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(confluenceBaseUrl + "/rest/api/space")
                    .queryParam("limit", 100)
                    .queryParam("expand", "permissions")
                    .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<ConfluenceSpace> spaces = new ArrayList<>();
            
            if (jsonResponse.has("results")) {
                JsonNode spacesNode = jsonResponse.get("results");
                for (JsonNode spaceNode : spacesNode) {
                    ConfluenceSpace space = new ConfluenceSpace();
                    space.setKey(spaceNode.get("key").asText());
                    space.setName(spaceNode.get("name").asText());
                    space.setType(spaceNode.get("type").asText());
                    spaces.add(space);
                }
            }
            
            return spaces;
            
        } catch (Exception e) {
            logger.error("Error fetching Confluence spaces for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private String buildCqlQuery(String userEmail, LocalDateTime startTime, LocalDateTime endTime, String dateField) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        return String.format(
            "creator = '%s' AND %s >= '%s' AND %s <= '%s'",
            userEmail,
            dateField,
            startTime.format(formatter),
            dateField,
            endTime.format(formatter)
        );
    }
    
    private String executeCqlQuery(String cql) {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
            (confluenceUsername + ":" + confluenceApiToken).getBytes()
        );
        
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(confluenceBaseUrl + "/rest/api/content/search")
                .queryParam("cql", cql)
                .queryParam("limit", 100)
                .queryParam("expand", "space,history,version,body.storage")
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .header("Content-Type", "application/json")
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
    
    private List<ConfluencePage> parsePagesFromResponse(String response) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(response);
        List<ConfluencePage> pages = new ArrayList<>();
        
        if (jsonResponse.has("results")) {
            JsonNode pagesNode = jsonResponse.get("results");
            for (JsonNode pageNode : pagesNode) {
                ConfluencePage page = new ConfluencePage();
                page.setId(pageNode.get("id").asText());
                page.setTitle(pageNode.get("title").asText());
                page.setType(pageNode.get("type").asText());
                
                if (pageNode.has("space")) {
                    page.setSpaceKey(pageNode.get("space").get("key").asText());
                }
                
                if (pageNode.has("history")) {
                    JsonNode historyNode = pageNode.get("history");
                    page.setCreatedDate(LocalDateTime.parse(
                        historyNode.get("createdDate").asText().substring(0, 19)
                    ));
                    
                    if (historyNode.has("createdBy")) {
                        page.setCreatedBy(historyNode.get("createdBy").get("email").asText());
                    }
                }
                
                if (pageNode.has("version")) {
                    JsonNode versionNode = pageNode.get("version");
                    page.setLastModified(LocalDateTime.parse(
                        versionNode.get("when").asText().substring(0, 19)
                    ));
                    
                    if (versionNode.has("by")) {
                        page.setLastModifiedBy(versionNode.get("by").get("email").asText());
                    }
                }
                
                if (pageNode.has("body") && pageNode.get("body").has("storage")) {
                    page.setContent(pageNode.get("body").get("storage").get("value").asText());
                }
                
                pages.add(page);
            }
        }
        
        return pages;
    }
    
    private List<ConfluenceComment> getPageComments(String pageId, String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching comments for page {} by user {}", pageId, userEmail);
        
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (confluenceUsername + ":" + confluenceApiToken).getBytes()
            );
            
            String response = webClient.get()
                .uri(confluenceBaseUrl + "/rest/api/content/" + pageId + "/child/comment")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<ConfluenceComment> comments = new ArrayList<>();
            
            if (jsonResponse.has("results")) {
                JsonNode commentsNode = jsonResponse.get("results");
                for (JsonNode commentNode : commentsNode) {
                    if (commentNode.has("history") && commentNode.get("history").has("createdBy")) {
                        String authorEmail = commentNode.get("history").get("createdBy").get("email").asText();
                        if (authorEmail.equals(userEmail)) {
                            LocalDateTime commentTime = LocalDateTime.parse(
                                commentNode.get("history").get("createdDate").asText().substring(0, 19)
                            );
                            
                            if (commentTime.isAfter(startTime) && commentTime.isBefore(endTime)) {
                                ConfluenceComment comment = new ConfluenceComment();
                                comment.setPageId(pageId);
                                comment.setAuthor(authorEmail);
                                comment.setContent(commentNode.get("body").get("storage").get("value").asText());
                                comment.setCreatedDate(commentTime);
                                comments.add(comment);
                            }
                        }
                    }
                }
            }
            
            return comments;
            
        } catch (Exception e) {
            logger.error("Error fetching comments for page {}: {}", pageId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private boolean isConfigured() {
        return confluenceBaseUrl != null && !confluenceBaseUrl.isEmpty() &&
               confluenceUsername != null && !confluenceUsername.isEmpty() &&
               confluenceApiToken != null && !confluenceApiToken.isEmpty();
    }
    
    public boolean testConnection() {
        logger.debug("Testing Confluence API connection");
        
        if (!isConfigured()) {
            logger.warn("Confluence configuration not complete");
            return false;
        }
        
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (confluenceUsername + ":" + confluenceApiToken).getBytes()
            );
            
            String response = webClient.get()
                .uri(confluenceBaseUrl + "/rest/api/user/current")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            boolean isValid = jsonResponse.has("accountId");
            
            if (isValid) {
                logger.info("Confluence API connection successful");
            } else {
                logger.error("Confluence API connection failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error testing Confluence API connection: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public static class ConfluencePage {
        private String id;
        private String title;
        private String type;
        private String spaceKey;
        private String content;
        private String createdBy;
        private LocalDateTime createdDate;
        private String lastModifiedBy;
        private LocalDateTime lastModified;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getSpaceKey() { return spaceKey; }
        public void setSpaceKey(String spaceKey) { this.spaceKey = spaceKey; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        
        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
        
        public String getLastModifiedBy() { return lastModifiedBy; }
        public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
        
        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
        
        @Override
        public String toString() {
            return "ConfluencePage{" +
                    "id='" + id + '\'' +
                    ", title='" + title + '\'' +
                    ", type='" + type + '\'' +
                    ", spaceKey='" + spaceKey + '\'' +
                    ", createdBy='" + createdBy + '\'' +
                    ", createdDate=" + createdDate +
                    ", lastModifiedBy='" + lastModifiedBy + '\'' +
                    ", lastModified=" + lastModified +
                    '}';
        }
    }
    
    public static class ConfluenceComment {
        private String pageId;
        private String author;
        private String content;
        private LocalDateTime createdDate;
        
        public String getPageId() { return pageId; }
        public void setPageId(String pageId) { this.pageId = pageId; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
        
        @Override
        public String toString() {
            return "ConfluenceComment{" +
                    "pageId='" + pageId + '\'' +
                    ", author='" + author + '\'' +
                    ", content='" + content + '\'' +
                    ", createdDate=" + createdDate +
                    '}';
        }
    }
    
    public static class ConfluenceSpace {
        private String key;
        private String name;
        private String type;
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        @Override
        public String toString() {
            return "ConfluenceSpace{" +
                    "key='" + key + '\'' +
                    ", name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
