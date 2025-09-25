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

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaIntegrationService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.ollama.model}")
    private String model;
    
    public OllamaIntegrationService(@Value("${app.ollama.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public String generateWorkSummary(String workData) {
        logger.info("Generating work summary using Ollama model: {}", model);
        
        String prompt = buildWorkSummaryPrompt(workData);
        return generateCompletion(prompt);
    }
    
    public String generateMeetingMinutes(String transcript) {
        logger.info("Generating meeting minutes using Ollama model: {}", model);
        
        String prompt = buildMeetingMinutesPrompt(transcript);
        return generateCompletion(prompt);
    }
    
    public String extractActionItems(String meetingContent) {
        logger.info("Extracting action items using Ollama model: {}", model);
        
        String prompt = buildActionItemsPrompt(meetingContent);
        return generateCompletion(prompt);
    }
    
    public String summarizeCustomerIssues(String issueData) {
        logger.info("Summarizing customer issues using Ollama model: {}", model);
        
        String prompt = buildCustomerIssuePrompt(issueData);
        return generateCompletion(prompt);
    }
    
    public String generateTeamSummary(String teamData) {
        logger.info("Generating team summary using Ollama model: {}", model);
        
        String prompt = buildTeamSummaryPrompt(teamData);
        return generateCompletion(prompt);
    }
    
    private String generateCompletion(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.7,
                "top_p", 0.9,
                "max_tokens", 1000
            ));
            
            String response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            String generatedText = jsonResponse.get("response").asText();
            
            logger.debug("Successfully generated completion with {} characters", generatedText.length());
            return generatedText;
            
        } catch (WebClientResponseException e) {
            logger.error("Error calling Ollama API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Error generating summary: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error calling Ollama API: {}", e.getMessage(), e);
            return "Error generating summary: " + e.getMessage();
        }
    }
    
    private String buildWorkSummaryPrompt(String workData) {
        return """
            Please analyze the following engineer work data and provide a concise, professional summary:
            
            Work Data:
            %s
            
            Please provide:
            1. Key accomplishments and contributions
            2. Collaboration highlights
            3. Technical achievements
            4. Areas of focus
            5. Overall productivity assessment
            
            Format the response as a clear, structured summary suitable for management review.
            """.formatted(workData);
    }
    
    private String buildMeetingMinutesPrompt(String transcript) {
        return """
            Please analyze the following meeting transcript and generate professional meeting minutes:
            
            Transcript:
            %s
            
            Please provide:
            1. Meeting summary
            2. Key discussion points
            3. Decisions made
            4. Action items with owners (if mentioned)
            5. Next steps
            
            Format as professional meeting minutes suitable for distribution to stakeholders.
            """.formatted(transcript);
    }
    
    private String buildActionItemsPrompt(String meetingContent) {
        return """
            Please extract action items from the following meeting content:
            
            Content:
            %s
            
            Please identify:
            1. Specific action items
            2. Responsible parties (if mentioned)
            3. Deadlines or timeframes (if mentioned)
            4. Priority level (if indicated)
            
            Format as a clear list of actionable items.
            """.formatted(meetingContent);
    }
    
    private String buildCustomerIssuePrompt(String issueData) {
        return """
            Please analyze the following customer issue resolution data:
            
            Issue Data:
            %s
            
            Please provide:
            1. Summary of issues resolved
            2. Customer impact assessment
            3. Resolution effectiveness
            4. Response time analysis
            5. Recommendations for improvement
            
            Format as a professional customer service summary.
            """.formatted(issueData);
    }
    
    private String buildTeamSummaryPrompt(String teamData) {
        return """
            Please analyze the following team performance data and generate a comprehensive summary:
            
            Team Data:
            %s
            
            Please provide:
            1. Team productivity overview
            2. Collaboration patterns
            3. Key achievements
            4. Areas for improvement
            5. Resource utilization
            6. Recommendations for team optimization
            
            Format as an executive summary suitable for leadership review.
            """.formatted(teamData);
    }
    
    public boolean isServiceAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            logger.warn("Ollama service is not available: {}", e.getMessage());
            return false;
        }
    }
}
