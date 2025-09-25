package com.engineerplatform.backend.controller;

import com.engineerplatform.backend.model.MeetingMinutes;
import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.service.MeetingMinutesService;
import com.engineerplatform.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MeetingController {
    
    private static final Logger logger = LoggerFactory.getLogger(MeetingController.class);
    
    private final MeetingMinutesService meetingMinutesService;
    private final UserService userService;
    
    @Autowired
    public MeetingController(MeetingMinutesService meetingMinutesService, UserService userService) {
        this.meetingMinutesService = meetingMinutesService;
        this.userService = userService;
    }
    
    @GetMapping("/my-meetings")
    public ResponseEntity<?> getMyMeetings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        
        logger.debug("Fetching meetings for user {} from {} to {}", 
                    authentication.getName(), startDate, endDate);
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            List<MeetingMinutes> meetings = meetingMinutesService.getUserMeetings(user.getId(), startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                "meetings", meetings,
                "totalCount", meetings.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching meetings for user {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch meetings: " + e.getMessage()));
        }
    }
    
    @GetMapping("/team-meetings")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEADER', 'ADMIN')")
    public ResponseEntity<?> getTeamMeetings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        
        logger.debug("Fetching team meetings for manager {} from {} to {}", 
                    authentication.getName(), startDate, endDate);
        
        try {
            User manager = userService.getUserByUsername(authentication.getName());
            List<MeetingMinutes> meetings = meetingMinutesService.getTeamMeetings(manager.getId(), startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                "meetings", meetings,
                "totalCount", meetings.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching team meetings for manager {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch team meetings: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{meetingId}")
    public ResponseEntity<?> getMeetingById(
            @PathVariable Long meetingId,
            Authentication authentication) {
        
        logger.debug("Fetching meeting {} for user {}", meetingId, authentication.getName());
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            MeetingMinutes meeting = meetingMinutesService.getMeetingById(meetingId);
            
            if (!meetingMinutesService.canUserAccessMeeting(user, meeting)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: You cannot view this meeting"));
            }
            
            return ResponseEntity.ok(meeting);
            
        } catch (Exception e) {
            logger.error("Error fetching meeting {} for user {}: {}", 
                        meetingId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch meeting: " + e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<?> searchMeetings(
            @RequestParam String query,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        
        logger.debug("Searching meetings for user {} with query: {}", authentication.getName(), query);
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            List<MeetingMinutes> meetings = meetingMinutesService.searchUserMeetings(
                user.getId(), query, startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                "meetings", meetings,
                "totalCount", meetings.size(),
                "query", query
            ));
            
        } catch (Exception e) {
            logger.error("Error searching meetings for user {} with query {}: {}", 
                        authentication.getName(), query, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to search meetings: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{meetingId}/regenerate")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEADER', 'ADMIN')")
    public ResponseEntity<?> regenerateMeetingMinutes(
            @PathVariable Long meetingId,
            Authentication authentication) {
        
        logger.info("Regenerating meeting minutes for meeting {} by user {}", 
                   meetingId, authentication.getName());
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            MeetingMinutes meeting = meetingMinutesService.getMeetingById(meetingId);
            
            if (!meetingMinutesService.canUserAccessMeeting(user, meeting)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: You cannot regenerate this meeting"));
            }
            
            MeetingMinutes regeneratedMeeting = meetingMinutesService.regenerateMeetingMinutes(meetingId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Meeting minutes regenerated successfully",
                "meeting", regeneratedMeeting
            ));
            
        } catch (Exception e) {
            logger.error("Error regenerating meeting minutes for meeting {} by user {}: {}", 
                        meetingId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to regenerate meeting minutes: " + e.getMessage()));
        }
    }
}
