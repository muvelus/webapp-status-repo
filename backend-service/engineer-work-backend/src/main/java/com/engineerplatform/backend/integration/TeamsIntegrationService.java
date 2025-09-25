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
public class TeamsIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TeamsIntegrationService.class);
    
    @Value("${app.integrations.teams.tenant-id:}")
    private String tenantId;
    
    @Value("${app.integrations.teams.client-id:}")
    private String clientId;
    
    @Value("${app.integrations.teams.client-secret:}")
    private String clientSecret;
    
    @Value("${app.integrations.teams.base-url:https://graph.microsoft.com/v1.0}")
    private String teamsBaseUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private LocalDateTime tokenExpiry;
    
    public TeamsIntegrationService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<TeamsMessage> getUserMessages(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Teams messages for user {} from {} to {}", userEmail, startTime, endTime);
        
        if (!isConfigured()) {
            logger.warn("Teams configuration not complete, skipping message fetch");
            return new ArrayList<>();
        }
        
        try {
            ensureValidToken();
            
            List<TeamsChannel> channels = getUserChannels(userEmail);
            List<TeamsMessage> allMessages = new ArrayList<>();
            
            for (TeamsChannel channel : channels) {
                List<TeamsMessage> channelMessages = getChannelMessages(channel.getId(), userEmail, startTime, endTime);
                allMessages.addAll(channelMessages);
            }
            
            logger.info("Retrieved {} Teams messages for user {}", allMessages.size(), userEmail);
            return allMessages;
            
        } catch (Exception e) {
            logger.error("Error fetching Teams messages for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<TeamsMeeting> getUserMeetings(String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Teams meetings for user {} from {} to {}", userEmail, startTime, endTime);
        
        if (!isConfigured()) {
            logger.warn("Teams configuration not complete, skipping meeting fetch");
            return new ArrayList<>();
        }
        
        try {
            ensureValidToken();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(teamsBaseUrl + "/users/" + userEmail + "/events")
                    .queryParam("$filter", String.format(
                        "start/dateTime ge '%s' and end/dateTime le '%s'",
                        startTime.format(formatter),
                        endTime.format(formatter)
                    ))
                    .queryParam("$select", "id,subject,start,end,organizer,attendees,onlineMeeting")
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            return parseMeetingsFromResponse(response);
            
        } catch (Exception e) {
            logger.error("Error fetching Teams meetings for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<TeamsChannel> getUserChannels(String userEmail) {
        logger.debug("Fetching Teams channels for user {}", userEmail);
        
        try {
            ensureValidToken();
            
            String response = webClient.get()
                .uri(teamsBaseUrl + "/users/" + userEmail + "/joinedTeams")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<TeamsChannel> channels = new ArrayList<>();
            
            if (jsonResponse.has("value")) {
                JsonNode teamsNode = jsonResponse.get("value");
                for (JsonNode teamNode : teamsNode) {
                    String teamId = teamNode.get("id").asText();
                    List<TeamsChannel> teamChannels = getTeamChannels(teamId);
                    channels.addAll(teamChannels);
                }
            }
            
            return channels;
            
        } catch (Exception e) {
            logger.error("Error fetching Teams channels for user {}: {}", userEmail, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private List<TeamsChannel> getTeamChannels(String teamId) {
        try {
            String response = webClient.get()
                .uri(teamsBaseUrl + "/teams/" + teamId + "/channels")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<TeamsChannel> channels = new ArrayList<>();
            
            if (jsonResponse.has("value")) {
                JsonNode channelsNode = jsonResponse.get("value");
                for (JsonNode channelNode : channelsNode) {
                    TeamsChannel channel = new TeamsChannel();
                    channel.setId(channelNode.get("id").asText());
                    channel.setDisplayName(channelNode.get("displayName").asText());
                    channel.setTeamId(teamId);
                    channels.add(channel);
                }
            }
            
            return channels;
            
        } catch (Exception e) {
            logger.error("Error fetching channels for team {}: {}", teamId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private List<TeamsMessage> getChannelMessages(String channelId, String userEmail, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching messages from channel {} for user {}", channelId, userEmail);
        
        try {
            String response = webClient.get()
                .uri(teamsBaseUrl + "/teams/" + channelId + "/channels/" + channelId + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<TeamsMessage> messages = new ArrayList<>();
            
            if (jsonResponse.has("value")) {
                JsonNode messagesNode = jsonResponse.get("value");
                for (JsonNode messageNode : messagesNode) {
                    if (messageNode.has("from") && 
                        messageNode.get("from").has("user") &&
                        messageNode.get("from").get("user").get("userPrincipalName").asText().equals(userEmail)) {
                        
                        LocalDateTime messageTime = LocalDateTime.parse(
                            messageNode.get("createdDateTime").asText().substring(0, 19)
                        );
                        
                        if (messageTime.isAfter(startTime) && messageTime.isBefore(endTime)) {
                            TeamsMessage message = new TeamsMessage();
                            message.setChannelId(channelId);
                            message.setUserEmail(userEmail);
                            message.setContent(messageNode.get("body").get("content").asText());
                            message.setCreatedDateTime(messageTime);
                            message.setMessageType(messageNode.get("messageType").asText());
                            messages.add(message);
                        }
                    }
                }
            }
            
            return messages;
            
        } catch (Exception e) {
            logger.error("Error fetching messages from channel {}: {}", channelId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private List<TeamsMeeting> parseMeetingsFromResponse(String response) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(response);
        List<TeamsMeeting> meetings = new ArrayList<>();
        
        if (jsonResponse.has("value")) {
            JsonNode meetingsNode = jsonResponse.get("value");
            for (JsonNode meetingNode : meetingsNode) {
                if (meetingNode.has("onlineMeeting") && !meetingNode.get("onlineMeeting").isNull()) {
                    TeamsMeeting meeting = new TeamsMeeting();
                    meeting.setId(meetingNode.get("id").asText());
                    meeting.setSubject(meetingNode.get("subject").asText());
                    meeting.setStartTime(LocalDateTime.parse(
                        meetingNode.get("start").get("dateTime").asText().substring(0, 19)
                    ));
                    meeting.setEndTime(LocalDateTime.parse(
                        meetingNode.get("end").get("dateTime").asText().substring(0, 19)
                    ));
                    
                    if (meetingNode.has("organizer")) {
                        meeting.setOrganizer(meetingNode.get("organizer").get("emailAddress").get("address").asText());
                    }
                    
                    meetings.add(meeting);
                }
            }
        }
        
        return meetings;
    }
    
    private void ensureValidToken() {
        if (accessToken == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
            refreshAccessToken();
        }
    }
    
    private void refreshAccessToken() {
        logger.debug("Refreshing Teams access token");
        
        try {
            String tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
            
            String requestBody = "grant_type=client_credentials" +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&scope=https://graph.microsoft.com/.default";
            
            String response = webClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            accessToken = jsonResponse.get("access_token").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();
            tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 300); // 5 min buffer
            
            logger.debug("Teams access token refreshed successfully");
            
        } catch (Exception e) {
            logger.error("Error refreshing Teams access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh Teams access token", e);
        }
    }
    
    private boolean isConfigured() {
        return tenantId != null && !tenantId.isEmpty() &&
               clientId != null && !clientId.isEmpty() &&
               clientSecret != null && !clientSecret.isEmpty();
    }
    
    public boolean testConnection() {
        logger.debug("Testing Teams API connection");
        
        if (!isConfigured()) {
            logger.warn("Teams configuration not complete");
            return false;
        }
        
        try {
            ensureValidToken();
            
            String response = webClient.get()
                .uri(teamsBaseUrl + "/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            boolean isValid = jsonResponse.has("id");
            
            if (isValid) {
                logger.info("Teams API connection successful");
            } else {
                logger.error("Teams API connection failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error testing Teams API connection: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public static class TeamsMessage {
        private String channelId;
        private String userEmail;
        private String content;
        private LocalDateTime createdDateTime;
        private String messageType;
        
        public String getChannelId() { return channelId; }
        public void setChannelId(String channelId) { this.channelId = channelId; }
        
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public LocalDateTime getCreatedDateTime() { return createdDateTime; }
        public void setCreatedDateTime(LocalDateTime createdDateTime) { this.createdDateTime = createdDateTime; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        
        @Override
        public String toString() {
            return "TeamsMessage{" +
                    "channelId='" + channelId + '\'' +
                    ", userEmail='" + userEmail + '\'' +
                    ", content='" + content + '\'' +
                    ", createdDateTime=" + createdDateTime +
                    ", messageType='" + messageType + '\'' +
                    '}';
        }
    }
    
    public static class TeamsMeeting {
        private String id;
        private String subject;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String organizer;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public String getOrganizer() { return organizer; }
        public void setOrganizer(String organizer) { this.organizer = organizer; }
        
        @Override
        public String toString() {
            return "TeamsMeeting{" +
                    "id='" + id + '\'' +
                    ", subject='" + subject + '\'' +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", organizer='" + organizer + '\'' +
                    '}';
        }
    }
    
    public static class TeamsChannel {
        private String id;
        private String displayName;
        private String teamId;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }
        
        @Override
        public String toString() {
            return "TeamsChannel{" +
                    "id='" + id + '\'' +
                    ", displayName='" + displayName + '\'' +
                    ", teamId='" + teamId + '\'' +
                    '}';
        }
    }
}
