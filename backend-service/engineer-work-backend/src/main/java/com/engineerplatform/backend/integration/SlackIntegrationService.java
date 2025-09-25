package com.engineerplatform.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlackIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SlackIntegrationService.class);
    
    @Value("${app.integrations.slack.token:}")
    private String slackToken;
    
    @Value("${app.integrations.slack.base-url:https://slack.com/api}")
    private String slackBaseUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public SlackIntegrationService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<SlackMessage> getUserMessages(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching Slack messages for user {} from {} to {}", userId, startTime, endTime);
        
        if (slackToken == null || slackToken.isEmpty()) {
            logger.warn("Slack token not configured, skipping message fetch");
            return new ArrayList<>();
        }
        
        try {
            List<SlackChannel> channels = getUserChannels(userId);
            List<SlackMessage> allMessages = new ArrayList<>();
            
            for (SlackChannel channel : channels) {
                List<SlackMessage> channelMessages = getChannelMessages(channel.getId(), userId, startTime, endTime);
                allMessages.addAll(channelMessages);
            }
            
            logger.info("Retrieved {} Slack messages for user {}", allMessages.size(), userId);
            return allMessages;
            
        } catch (Exception e) {
            logger.error("Error fetching Slack messages for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<SlackChannel> getUserChannels(String userId) {
        logger.debug("Fetching Slack channels for user {}", userId);
        
        try {
            String response = webClient.get()
                .uri(slackBaseUrl + "/conversations.list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + slackToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<SlackChannel> channels = new ArrayList<>();
            
            if (jsonResponse.get("ok").asBoolean()) {
                JsonNode channelsNode = jsonResponse.get("channels");
                for (JsonNode channelNode : channelsNode) {
                    SlackChannel channel = new SlackChannel();
                    channel.setId(channelNode.get("id").asText());
                    channel.setName(channelNode.get("name").asText());
                    channel.setIsPrivate(channelNode.get("is_private").asBoolean());
                    channels.add(channel);
                }
            } else {
                logger.error("Slack API error: {}", jsonResponse.get("error").asText());
            }
            
            return channels;
            
        } catch (Exception e) {
            logger.error("Error fetching Slack channels for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<SlackMessage> getChannelMessages(String channelId, String userId, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching messages from channel {} for user {} from {} to {}", channelId, userId, startTime, endTime);
        
        try {
            long oldest = startTime.toEpochSecond(ZoneOffset.UTC);
            long latest = endTime.toEpochSecond(ZoneOffset.UTC);
            
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(slackBaseUrl + "/conversations.history")
                    .queryParam("channel", channelId)
                    .queryParam("oldest", oldest)
                    .queryParam("latest", latest)
                    .queryParam("limit", 1000)
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + slackToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<SlackMessage> messages = new ArrayList<>();
            
            if (jsonResponse.get("ok").asBoolean()) {
                JsonNode messagesNode = jsonResponse.get("messages");
                for (JsonNode messageNode : messagesNode) {
                    if (messageNode.has("user") && messageNode.get("user").asText().equals(userId)) {
                        SlackMessage message = new SlackMessage();
                        message.setChannelId(channelId);
                        message.setUserId(messageNode.get("user").asText());
                        message.setText(messageNode.get("text").asText());
                        message.setTimestamp(LocalDateTime.ofEpochSecond(
                            Long.parseLong(messageNode.get("ts").asText().split("\\.")[0]),
                            0, ZoneOffset.UTC));
                        message.setMessageType(messageNode.has("subtype") ? 
                            messageNode.get("subtype").asText() : "message");
                        messages.add(message);
                    }
                }
            } else {
                logger.error("Slack API error for channel {}: {}", channelId, jsonResponse.get("error").asText());
            }
            
            return messages;
            
        } catch (Exception e) {
            logger.error("Error fetching messages from channel {} for user {}: {}", channelId, userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public List<SlackMessage> getDirectMessages(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching direct messages for user {} from {} to {}", userId, startTime, endTime);
        
        try {
            String response = webClient.get()
                .uri(slackBaseUrl + "/conversations.list?types=im")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + slackToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            List<SlackMessage> allDMs = new ArrayList<>();
            
            if (jsonResponse.get("ok").asBoolean()) {
                JsonNode channelsNode = jsonResponse.get("channels");
                for (JsonNode channelNode : channelsNode) {
                    String dmChannelId = channelNode.get("id").asText();
                    List<SlackMessage> dmMessages = getChannelMessages(dmChannelId, userId, startTime, endTime);
                    allDMs.addAll(dmMessages);
                }
            }
            
            return allDMs;
            
        } catch (Exception e) {
            logger.error("Error fetching direct messages for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    public boolean testConnection() {
        logger.debug("Testing Slack API connection");
        
        if (slackToken == null || slackToken.isEmpty()) {
            logger.warn("Slack token not configured");
            return false;
        }
        
        try {
            String response = webClient.get()
                .uri(slackBaseUrl + "/auth.test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + slackToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            boolean isOk = jsonResponse.get("ok").asBoolean();
            
            if (isOk) {
                logger.info("Slack API connection successful");
            } else {
                logger.error("Slack API connection failed: {}", jsonResponse.get("error").asText());
            }
            
            return isOk;
            
        } catch (Exception e) {
            logger.error("Error testing Slack API connection: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public static class SlackMessage {
        private String channelId;
        private String userId;
        private String text;
        private LocalDateTime timestamp;
        private String messageType;
        
        public String getChannelId() { return channelId; }
        public void setChannelId(String channelId) { this.channelId = channelId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        
        @Override
        public String toString() {
            return "SlackMessage{" +
                    "channelId='" + channelId + '\'' +
                    ", userId='" + userId + '\'' +
                    ", text='" + text + '\'' +
                    ", timestamp=" + timestamp +
                    ", messageType='" + messageType + '\'' +
                    '}';
        }
    }
    
    public static class SlackChannel {
        private String id;
        private String name;
        private boolean isPrivate;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public boolean isPrivate() { return isPrivate; }
        public void setIsPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
        
        @Override
        public String toString() {
            return "SlackChannel{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", isPrivate=" + isPrivate +
                    '}';
        }
    }
}
