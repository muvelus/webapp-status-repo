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
import java.util.List;

@Service
public class GoogleIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleIntegrationService.class);
    
    @Value("${app.integrations.google.client-id:}")
    private String googleClientId;
    
    @Value("${app.integrations.google.client-secret:}")
    private String googleClientSecret;
    
    @Value("${app.integrations.google.refresh-token:}")
    private String googleRefreshToken;
    
    @Value("${app.integrations.google.base-url:https://www.googleapis.com}")
    private String googleBaseUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private LocalDateTime tokenExpiry;
    
    public GoogleIntegrationService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<GoogleDocument> getUserDocuments(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Google Docs for user {} from {} to {}", userEmail, startTime, endTime);
        
        if (!isConfigured()) {
            logger.warn("Google configuration not complete, skipping document fetch");
            return new ArrayList<>();
        }
        
        try {
            ensureValidToken();
            
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(googleBaseUrl + "/drive/v3/files")
                    .queryParam("q", String.format(
                        "mimeType='application/vnd.google-apps.document' and modifiedTime>'%s' and modifiedTime<'%s'",
                        startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
                        endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
                    ))
                    .queryParam("fields", "files(id,name,createdTime,modifiedTime,owners,lastModifyingUser)")
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            List<GoogleDocument> documents = parseDocumentsFromResponse(response, userEmail);
            logger.info("Retrieved {} Google Docs for user {}", documents.size(), userEmail);
            return documents;
            
        } catch (Exception e) {
            logger.error("Error fetching Google Docs for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<GoogleCalendarEvent> getUserCalendarEvents(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Google Calendar events for user {} from {} to {}", userEmail, startTime, endTime);
        
        try {
            ensureValidToken();
            
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(googleBaseUrl + "/calendar/v3/calendars/primary/events")
                    .queryParam("timeMin", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z")
                    .queryParam("timeMax", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z")
                    .queryParam("singleEvents", "true")
                    .queryParam("orderBy", "startTime")
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            List<GoogleCalendarEvent> events = parseCalendarEventsFromResponse(response);
            logger.info("Retrieved {} Google Calendar events for user {}", events.size(), userEmail);
            return events;
            
        } catch (Exception e) {
            logger.error("Error fetching Google Calendar events for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<GmailMessage> getUserEmails(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Gmail messages for user {} from {} to {}", userEmail, startTime, endTime);
        
        try {
            ensureValidToken();
            
            long startEpoch = startTime.toEpochSecond(java.time.ZoneOffset.UTC);
            long endEpoch = endTime.toEpochSecond(java.time.ZoneOffset.UTC);
            
            String query = String.format("after:%d before:%d", startEpoch, endEpoch);
            
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(googleBaseUrl + "/gmail/v1/users/me/messages")
                    .queryParam("q", query)
                    .queryParam("maxResults", 100)
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            List<GmailMessage> messages = parseGmailMessagesFromResponse(response);
            logger.info("Retrieved {} Gmail messages for user {}", messages.size(), userEmail);
            return messages;
            
        } catch (Exception e) {
            logger.error("Error fetching Gmail messages for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public String getDocumentContent(String documentId) {
        logger.debug("Fetching content for Google Doc: {}", documentId);
        
        try {
            ensureValidToken();
            
            String response = webClient.get()
                .uri(googleBaseUrl + "/docs/v1/documents/" + documentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            StringBuilder content = new StringBuilder();
            
            if (jsonResponse.has("body") && jsonResponse.get("body").has("content")) {
                JsonNode contentNode = jsonResponse.get("body").get("content");
                for (JsonNode element : contentNode) {
                    if (element.has("paragraph") && element.get("paragraph").has("elements")) {
                        JsonNode elementsNode = element.get("paragraph").get("elements");
                        for (JsonNode textElement : elementsNode) {
                            if (textElement.has("textRun") && textElement.get("textRun").has("content")) {
                                content.append(textElement.get("textRun").get("content").asText());
                            }
                        }
                    }
                }
            }
            
            return content.toString();
            
        } catch (Exception e) {
            logger.error("Error fetching content for document {}: {}", documentId, e.getMessage(), e);
            return "";
        }
    }
    
    private List<GoogleDocument> parseDocumentsFromResponse(String response, String userEmail) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(response);
        List<GoogleDocument> documents = new ArrayList<>();
        
        if (jsonResponse.has("files")) {
            JsonNode filesNode = jsonResponse.get("files");
            for (JsonNode fileNode : filesNode) {
                boolean isUserDocument = false;
                
                if (fileNode.has("owners")) {
                    for (JsonNode owner : fileNode.get("owners")) {
                        if (owner.get("emailAddress").asText().equals(userEmail)) {
                            isUserDocument = true;
                            break;
                        }
                    }
                }
                
                if (fileNode.has("lastModifyingUser") && 
                    fileNode.get("lastModifyingUser").get("emailAddress").asText().equals(userEmail)) {
                    isUserDocument = true;
                }
                
                if (isUserDocument) {
                    GoogleDocument document = new GoogleDocument();
                    document.setId(fileNode.get("id").asText());
                    document.setName(fileNode.get("name").asText());
                    document.setCreatedTime(LocalDateTime.parse(
                        fileNode.get("createdTime").asText().substring(0, 19)
                    ));
                    document.setModifiedTime(LocalDateTime.parse(
                        fileNode.get("modifiedTime").asText().substring(0, 19)
                    ));
                    
                    if (fileNode.has("lastModifyingUser")) {
                        document.setLastModifyingUser(
                            fileNode.get("lastModifyingUser").get("emailAddress").asText()
                        );
                    }
                    
                    documents.add(document);
                }
            }
        }
        
        return documents;
    }
    
    private List<GoogleCalendarEvent> parseCalendarEventsFromResponse(String response) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(response);
        List<GoogleCalendarEvent> events = new ArrayList<>();
        
        if (jsonResponse.has("items")) {
            JsonNode itemsNode = jsonResponse.get("items");
            for (JsonNode eventNode : itemsNode) {
                GoogleCalendarEvent event = new GoogleCalendarEvent();
                event.setId(eventNode.get("id").asText());
                event.setSummary(eventNode.get("summary").asText());
                
                if (eventNode.has("description")) {
                    event.setDescription(eventNode.get("description").asText());
                }
                
                if (eventNode.has("start") && eventNode.get("start").has("dateTime")) {
                    event.setStartTime(LocalDateTime.parse(
                        eventNode.get("start").get("dateTime").asText().substring(0, 19)
                    ));
                }
                
                if (eventNode.has("end") && eventNode.get("end").has("dateTime")) {
                    event.setEndTime(LocalDateTime.parse(
                        eventNode.get("end").get("dateTime").asText().substring(0, 19)
                    ));
                }
                
                if (eventNode.has("organizer")) {
                    event.setOrganizer(eventNode.get("organizer").get("email").asText());
                }
                
                events.add(event);
            }
        }
        
        return events;
    }
    
    private List<GmailMessage> parseGmailMessagesFromResponse(String response) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(response);
        List<GmailMessage> messages = new ArrayList<>();
        
        if (jsonResponse.has("messages")) {
            JsonNode messagesNode = jsonResponse.get("messages");
            for (JsonNode messageNode : messagesNode) {
                String messageId = messageNode.get("id").asText();
                GmailMessage message = getGmailMessageDetails(messageId);
                if (message != null) {
                    messages.add(message);
                }
            }
        }
        
        return messages;
    }
    
    private GmailMessage getGmailMessageDetails(String messageId) {
        try {
            String response = webClient.get()
                .uri(googleBaseUrl + "/gmail/v1/users/me/messages/" + messageId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            GmailMessage message = new GmailMessage();
            message.setId(messageId);
            
            if (jsonResponse.has("payload")) {
                JsonNode payloadNode = jsonResponse.get("payload");
                
                if (payloadNode.has("headers")) {
                    for (JsonNode header : payloadNode.get("headers")) {
                        String name = header.get("name").asText();
                        String value = header.get("value").asText();
                        
                        switch (name) {
                            case "Subject":
                                message.setSubject(value);
                                break;
                            case "From":
                                message.setFrom(value);
                                break;
                            case "To":
                                message.setTo(value);
                                break;
                            case "Date":
                                message.setDate(value);
                                break;
                        }
                    }
                }
                
                if (payloadNode.has("body") && payloadNode.get("body").has("data")) {
                    String encodedBody = payloadNode.get("body").get("data").asText();
                    message.setBody(new String(java.util.Base64.getDecoder().decode(encodedBody)));
                }
            }
            
            return message;
            
        } catch (Exception e) {
            logger.error("Error fetching Gmail message details for {}: {}", messageId, e.getMessage(), e);
            return null;
        }
    }
    
    private void ensureValidToken() {
        if (accessToken == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
            refreshAccessToken();
        }
    }
    
    private void refreshAccessToken() {
        logger.debug("Refreshing Google access token");
        
        try {
            String requestBody = "grant_type=refresh_token" +
                "&client_id=" + googleClientId +
                "&client_secret=" + googleClientSecret +
                "&refresh_token=" + googleRefreshToken;
            
            String response = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            accessToken = jsonResponse.get("access_token").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();
            tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 300);
            
            logger.debug("Google access token refreshed successfully");
            
        } catch (Exception e) {
            logger.error("Error refreshing Google access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh Google access token", e);
        }
    }
    
    private boolean isConfigured() {
        return googleClientId != null && !googleClientId.isEmpty() &&
               googleClientSecret != null && !googleClientSecret.isEmpty() &&
               googleRefreshToken != null && !googleRefreshToken.isEmpty();
    }
    
    public boolean testConnection() {
        logger.debug("Testing Google API connection");
        
        if (!isConfigured()) {
            logger.warn("Google configuration not complete");
            return false;
        }
        
        try {
            ensureValidToken();
            
            String response = webClient.get()
                .uri(googleBaseUrl + "/oauth2/v1/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            boolean isValid = jsonResponse.has("id");
            
            if (isValid) {
                logger.info("Google API connection successful");
            } else {
                logger.error("Google API connection failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error testing Google API connection: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public static class GoogleDocument {
        private String id;
        private String name;
        private LocalDateTime createdTime;
        private LocalDateTime modifiedTime;
        private String lastModifyingUser;
        private String content;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
        
        public LocalDateTime getModifiedTime() { return modifiedTime; }
        public void setModifiedTime(LocalDateTime modifiedTime) { this.modifiedTime = modifiedTime; }
        
        public String getLastModifyingUser() { return lastModifyingUser; }
        public void setLastModifyingUser(String lastModifyingUser) { this.lastModifyingUser = lastModifyingUser; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        @Override
        public String toString() {
            return "GoogleDocument{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", createdTime=" + createdTime +
                    ", modifiedTime=" + modifiedTime +
                    ", lastModifyingUser='" + lastModifyingUser + '\'' +
                    '}';
        }
    }
    
    public static class GoogleCalendarEvent {
        private String id;
        private String summary;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String organizer;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public String getOrganizer() { return organizer; }
        public void setOrganizer(String organizer) { this.organizer = organizer; }
        
        @Override
        public String toString() {
            return "GoogleCalendarEvent{" +
                    "id='" + id + '\'' +
                    ", summary='" + summary + '\'' +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", organizer='" + organizer + '\'' +
                    '}';
        }
    }
    
    public static class GmailMessage {
        private String id;
        private String subject;
        private String from;
        private String to;
        private String date;
        private String body;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        @Override
        public String toString() {
            return "GmailMessage{" +
                    "id='" + id + '\'' +
                    ", subject='" + subject + '\'' +
                    ", from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    ", date='" + date + '\'' +
                    '}';
        }
    }
}
