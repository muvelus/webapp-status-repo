package com.engineerplatform.backend.repository;

import com.engineerplatform.backend.model.ReportSchedule;
import com.engineerplatform.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {
    
    List<ReportSchedule> findByUser(User user);
    
    List<ReportSchedule> findByUserAndActive(User user, boolean active);
    
    List<ReportSchedule> findByActive(boolean active);
    
    List<ReportSchedule> findByReportType(ReportSchedule.ReportType reportType);
    
    List<ReportSchedule> findByFrequency(ReportSchedule.Frequency frequency);
    
    @Query("SELECT rs FROM ReportSchedule rs WHERE rs.active = true AND rs.nextScheduled <= :currentTime")
    List<ReportSchedule> findSchedulesDueForExecution(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT rs FROM ReportSchedule rs WHERE rs.user = :user AND rs.reportType = :reportType AND rs.active = true")
    List<ReportSchedule> findActiveSchedulesByUserAndType(
        @Param("user") User user, 
        @Param("reportType") ReportSchedule.ReportType reportType);
    
    @Query("SELECT rs FROM ReportSchedule rs WHERE rs.recipientEmail = :email AND rs.active = true")
    List<ReportSchedule> findActiveSchedulesByRecipientEmail(@Param("email") String email);
}
