package com.engineerplatform.backend.controller;

import com.engineerplatform.backend.dto.WorkSummaryDto;
import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.model.WorkSummary;
import com.engineerplatform.backend.service.UserService;
import com.engineerplatform.backend.service.WorkSummaryService;
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
@RequestMapping("/api/work-summary")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WorkSummaryController {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkSummaryController.class);
    
    private final WorkSummaryService workSummaryService;
    private final UserService userService;
    
    @Autowired
    public WorkSummaryController(WorkSummaryService workSummaryService, UserService userService) {
        this.workSummaryService = workSummaryService;
        this.userService = userService;
    }
    
    @PostMapping("/generate/daily")
    public ResponseEntity<?> generateDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        
        logger.info("Generating daily summary for user {} on date {}", authentication.getName(), date);
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            WorkSummary summary = workSummaryService.generateDailySummary(user.getId(), date);
            WorkSummaryDto dto = workSummaryService.convertToDto(summary);
            
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            logger.error("Error generating daily summary for user {} on date {}: {}", 
                        authentication.getName(), date, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to generate daily summary: " + e.getMessage()));
        }
    }
    
    @PostMapping("/generate/weekly")
    public ResponseEntity<?> generateWeeklySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            Authentication authentication) {
        
        logger.info("Generating weekly summary for user {} starting from {}", 
                   authentication.getName(), weekStartDate);
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            WorkSummary summary = workSummaryService.generateWeeklySummary(user.getId(), weekStartDate);
            WorkSummaryDto dto = workSummaryService.convertToDto(summary);
            
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            logger.error("Error generating weekly summary for user {} starting from {}: {}", 
                        authentication.getName(), weekStartDate, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to generate weekly summary: " + e.getMessage()));
        }
    }
    
    @GetMapping("/my-summaries")
    public ResponseEntity<?> getMySummaries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        
        logger.debug("Fetching summaries for user {} from {} to {}", 
                    authentication.getName(), startDate, endDate);
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            List<WorkSummary> summaries = workSummaryService.getUserSummaries(user.getId(), startDate, endDate);
            List<WorkSummaryDto> dtos = workSummaryService.convertToDtoList(summaries);
            
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching summaries for user {} from {} to {}: {}", 
                        authentication.getName(), startDate, endDate, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch summaries: " + e.getMessage()));
        }
    }
    
    @GetMapping("/team-summaries")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEADER', 'ADMIN')")
    public ResponseEntity<?> getTeamSummaries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        
        logger.debug("Fetching team summaries for manager {} from {} to {}", 
                    authentication.getName(), startDate, endDate);
        
        try {
            User manager = userService.getUserByUsername(authentication.getName());
            List<WorkSummary> summaries = workSummaryService.getTeamSummaries(manager.getId(), startDate, endDate);
            List<WorkSummaryDto> dtos = workSummaryService.convertToDtoList(summaries);
            
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching team summaries for manager {} from {} to {}: {}", 
                        authentication.getName(), startDate, endDate, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch team summaries: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}/summaries")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEADER', 'ADMIN')")
    public ResponseEntity<?> getUserSummaries(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        
        logger.debug("Fetching summaries for user {} by manager {} from {} to {}", 
                    userId, authentication.getName(), startDate, endDate);
        
        try {
            User manager = userService.getUserByUsername(authentication.getName());
            User targetUser = userService.getUserById(userId);
            
            if (!userService.canManagerAccessUser(manager, targetUser)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: You cannot view this user's summaries"));
            }
            
            List<WorkSummary> summaries = workSummaryService.getUserSummaries(userId, startDate, endDate);
            List<WorkSummaryDto> dtos = workSummaryService.convertToDtoList(summaries);
            
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching summaries for user {} by manager {} from {} to {}: {}", 
                        userId, authentication.getName(), startDate, endDate, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch user summaries: " + e.getMessage()));
        }
    }
    
    @GetMapping("/summary/{summaryId}")
    public ResponseEntity<?> getSummaryById(
            @PathVariable Long summaryId,
            Authentication authentication) {
        
        logger.debug("Fetching summary {} for user {}", summaryId, authentication.getName());
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            WorkSummary summary = workSummaryService.getSummaryById(summaryId);
            
            if (!summary.getUser().getId().equals(user.getId()) && 
                !userService.canManagerAccessUser(user, summary.getUser())) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: You cannot view this summary"));
            }
            
            WorkSummaryDto dto = workSummaryService.convertToDto(summary);
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            logger.error("Error fetching summary {} for user {}: {}", 
                        summaryId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch summary: " + e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard/today")
    public ResponseEntity<?> getTodayDashboard(Authentication authentication) {
        logger.debug("Fetching today's dashboard for user {}", authentication.getName());
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            LocalDate today = LocalDate.now();
            
            List<WorkSummary> summaries = workSummaryService.getUserSummaries(user.getId(), today, today);
            
            if (summaries.isEmpty()) {
                WorkSummary todaySummary = workSummaryService.generateDailySummary(user.getId(), today);
                summaries = List.of(todaySummary);
            }
            
            List<WorkSummaryDto> dtos = workSummaryService.convertToDtoList(summaries);
            return ResponseEntity.ok(Map.of(
                "summaries", dtos,
                "date", today,
                "user", user.getUsername()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching today's dashboard for user {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch dashboard: " + e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard/week")
    public ResponseEntity<?> getWeekDashboard(Authentication authentication) {
        logger.debug("Fetching this week's dashboard for user {}", authentication.getName());
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            
            List<WorkSummary> summaries = workSummaryService.getUserSummaries(user.getId(), weekStart, today);
            List<WorkSummaryDto> dtos = workSummaryService.convertToDtoList(summaries);
            
            return ResponseEntity.ok(Map.of(
                "summaries", dtos,
                "weekStart", weekStart,
                "weekEnd", today,
                "user", user.getUsername()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching this week's dashboard for user {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch week dashboard: " + e.getMessage()));
        }
    }
}
