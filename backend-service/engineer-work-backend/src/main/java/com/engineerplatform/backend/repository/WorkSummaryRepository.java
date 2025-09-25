package com.engineerplatform.backend.repository;

import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.model.WorkSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkSummaryRepository extends JpaRepository<WorkSummary, Long> {
    
    List<WorkSummary> findByUser(User user);
    
    List<WorkSummary> findByUserAndSummaryType(User user, WorkSummary.SummaryType summaryType);
    
    Optional<WorkSummary> findByUserAndSummaryDateAndSummaryType(
        User user, LocalDate summaryDate, WorkSummary.SummaryType summaryType);
    
    @Query("SELECT ws FROM WorkSummary ws WHERE ws.user = :user AND ws.summaryDate BETWEEN :startDate AND :endDate")
    List<WorkSummary> findByUserAndDateRange(
        @Param("user") User user, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ws FROM WorkSummary ws WHERE ws.user IN :users AND ws.summaryDate = :date")
    List<WorkSummary> findByUsersAndDate(
        @Param("users") List<User> users, 
        @Param("date") LocalDate date);
    
    @Query("SELECT ws FROM WorkSummary ws WHERE ws.user IN :users AND ws.summaryDate BETWEEN :startDate AND :endDate")
    List<WorkSummary> findByUsersAndDateRange(
        @Param("users") List<User> users, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ws FROM WorkSummary ws WHERE ws.summaryDate = :date AND ws.summaryType = :summaryType")
    List<WorkSummary> findBySummaryDateAndType(
        @Param("date") LocalDate date, 
        @Param("summaryType") WorkSummary.SummaryType summaryType);
    
    @Query("SELECT ws FROM WorkSummary ws WHERE ws.user.manager = :manager AND ws.summaryDate BETWEEN :startDate AND :endDate")
    List<WorkSummary> findByManagerAndDateRange(
        @Param("manager") User manager, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT AVG(ws.productivityScore) FROM WorkSummary ws WHERE ws.user = :user AND ws.summaryDate BETWEEN :startDate AND :endDate")
    Double getAverageProductivityScore(
        @Param("user") User user, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT AVG(ws.collaborationScore) FROM WorkSummary ws WHERE ws.user = :user AND ws.summaryDate BETWEEN :startDate AND :endDate")
    Double getAverageCollaborationScore(
        @Param("user") User user, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
}
