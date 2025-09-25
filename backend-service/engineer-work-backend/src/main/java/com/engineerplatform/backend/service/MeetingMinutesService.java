package com.engineerplatform.backend.service;

import com.engineerplatform.backend.integration.OllamaIntegrationService;
import com.engineerplatform.backend.integration.TeamsIntegrationService;
import com.engineerplatform.backend.model.MeetingMinutes;
import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.repository.MeetingMinutesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeetingMinutesService {
    
    private static final Logger logger = LoggerFactory.getLogger(MeetingMinutesService.class);
    
    private final MeetingMinutesRepository meetingMinutesRepository;
    private final OllamaIntegrationService ollamaService;
    private final TeamsIntegrationService teamsService;
    
    @Autowired
    public MeetingMinutesService(MeetingMinutesRepository meetingMinutesRepository,
                                OllamaIntegrationService ollamaService,
                                TeamsIntegrationService teamsService) {
        this.meetingMinutesRepository = meetingMinutesRepository;
        this.ollamaService = ollamaService;
        this.teamsService = teamsService;
    }
    
    public List<MeetingMinutes> getUserMeetings(Long userId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching meetings for user {} from {} to {}", userId, startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        return meetingMinutesRepository.findByParticipantIdAndDateRange(userId, startDateTime, endDateTime);
    }
    
    public List<MeetingMinutes> getTeamMeetings(Long managerId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching team meetings for manager {} from {} to {}", managerId, startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        return meetingMinutesRepository.findTeamMeetingsByManagerId(managerId, startDateTime, endDateTime);
    }
    
    public MeetingMinutes getMeetingById(Long meetingId) {
        logger.debug("Fetching meeting by ID: {}", meetingId);
        
        return meetingMinutesRepository.findById(meetingId)
            .orElseThrow(() -> new RuntimeException("Meeting not found with ID: " + meetingId));
    }
    
    public List<MeetingMinutes> searchUserMeetings(Long userId, String query, LocalDate startDate, LocalDate endDate) {
        logger.debug("Searching meetings for user {} with query: {}", userId, query);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        return meetingMinutesRepository.searchByParticipantAndContent(userId, query, startDateTime, endDateTime);
    }
    
    public boolean canUserAccessMeeting(User user, MeetingMinutes meeting) {
        if (user.getRole() == User.Role.ADMIN) {
            return true;
        }
        
        if (meeting.getParticipants().contains(user)) {
            return true;
        }
        
        if (user.getRole() == User.Role.MANAGER || user.getRole() == User.Role.LEADER) {
            for (User participant : meeting.getParticipants()) {
                if (participant.getManager() != null && 
                    participant.getManager().getId().equals(user.getId())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public MeetingMinutes generateMeetingMinutes(String meetingId, String transcript, 
                                               List<String> attendees, LocalDateTime meetingDate) {
        logger.info("Generating meeting minutes for meeting: {}", meetingId);
        
        try {
            String prompt = buildMeetingMinutesPrompt(transcript, attendees);
            String generatedMinutes = ollamaService.generateMeetingMinutes(transcript);
            
            MeetingMinutes meetingMinutes = new MeetingMinutes();
            meetingMinutes.setMeetingId(meetingId);
            meetingMinutes.setMeetingDate(meetingDate);
            meetingMinutes.setAttendees(String.join(", ", attendees));
            meetingMinutes.setTranscript(transcript);
            meetingMinutes.setAiSummary(generatedMinutes);
            meetingMinutes.setKeyPoints(extractKeyPoints(generatedMinutes));
            meetingMinutes.setActionItems(extractActionItems(generatedMinutes));
            meetingMinutes.setDecisionsMade(extractDecisions(generatedMinutes));
            
            MeetingMinutes savedMeeting = meetingMinutesRepository.save(meetingMinutes);
            logger.info("Successfully generated and saved meeting minutes for meeting: {}", meetingId);
            
            return savedMeeting;
            
        } catch (Exception e) {
            logger.error("Error generating meeting minutes for meeting {}: {}", meetingId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate meeting minutes", e);
        }
    }
    
    public MeetingMinutes regenerateMeetingMinutes(Long meetingId) {
        logger.info("Regenerating meeting minutes for meeting ID: {}", meetingId);
        
        MeetingMinutes existingMeeting = getMeetingById(meetingId);
        
        if (existingMeeting.getTranscript() == null || existingMeeting.getTranscript().isEmpty()) {
            throw new RuntimeException("Cannot regenerate meeting minutes: No transcript available");
        }
        
        String prompt = buildMeetingMinutesPrompt(existingMeeting.getTranscript(), 
                                                List.of(existingMeeting.getAttendees().split(", ")));
        String regeneratedMinutes = ollamaService.generateMeetingMinutes(existingMeeting.getTranscript());
        
        existingMeeting.setAiSummary(regeneratedMinutes);
        existingMeeting.setKeyPoints(extractKeyPoints(regeneratedMinutes));
        existingMeeting.setActionItems(extractActionItems(regeneratedMinutes));
        existingMeeting.setDecisionsMade(extractDecisions(regeneratedMinutes));
        existingMeeting.setUpdatedAt(LocalDateTime.now());
        
        return meetingMinutesRepository.save(existingMeeting);
    }
    
    private String buildMeetingMinutesPrompt(String transcript, List<String> attendees) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please analyze the following meeting transcript and generate comprehensive meeting minutes.\n\n");
        prompt.append("Meeting Attendees: ").append(String.join(", ", attendees)).append("\n\n");
        prompt.append("Transcript:\n").append(transcript).append("\n\n");
        prompt.append("Please provide:\n");
        prompt.append("1. A summary of key discussion points\n");
        prompt.append("2. Important decisions made\n");
        prompt.append("3. Action items with responsible parties (if mentioned)\n");
        prompt.append("4. Next steps or follow-up items\n\n");
        prompt.append("Format the response in a clear, professional manner suitable for distribution to stakeholders.");
        
        return prompt.toString();
    }
    
    private String extractKeyPoints(String generatedMinutes) {
        try {
            String keyPointsPrompt = "Extract only the key discussion points from the following meeting minutes:\n\n" + 
                                   generatedMinutes + "\n\nProvide a bulleted list of key points discussed.";
            return ollamaService.extractActionItems(keyPointsPrompt);
        } catch (Exception e) {
            logger.warn("Failed to extract key points, using fallback method: {}", e.getMessage());
            return extractSectionByKeyword(generatedMinutes, "key", "discussion", "points");
        }
    }
    
    private String extractActionItems(String generatedMinutes) {
        try {
            String actionItemsPrompt = "Extract only the action items from the following meeting minutes:\n\n" + 
                                     generatedMinutes + "\n\nProvide a bulleted list of action items with responsible parties if mentioned.";
            return ollamaService.extractActionItems(actionItemsPrompt);
        } catch (Exception e) {
            logger.warn("Failed to extract action items, using fallback method: {}", e.getMessage());
            return extractSectionByKeyword(generatedMinutes, "action", "items", "todo");
        }
    }
    
    private String extractDecisions(String generatedMinutes) {
        try {
            String decisionsPrompt = "Extract only the decisions made from the following meeting minutes:\n\n" + 
                                   generatedMinutes + "\n\nProvide a bulleted list of decisions made during the meeting.";
            return ollamaService.extractActionItems(decisionsPrompt);
        } catch (Exception e) {
            logger.warn("Failed to extract decisions, using fallback method: {}", e.getMessage());
            return extractSectionByKeyword(generatedMinutes, "decision", "decided", "agreed");
        }
    }
    
    private String extractSectionByKeyword(String text, String... keywords) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            for (String keyword : keywords) {
                if (lowerLine.contains(keyword.toLowerCase())) {
                    result.append(line.trim()).append("\n");
                    break;
                }
            }
        }
        
        return result.length() > 0 ? result.toString() : "No specific items found.";
    }
}
