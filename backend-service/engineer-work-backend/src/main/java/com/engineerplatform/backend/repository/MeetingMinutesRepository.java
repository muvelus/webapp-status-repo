package com.engineerplatform.backend.repository;

import com.engineerplatform.backend.model.MeetingMinutes;
import com.engineerplatform.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, Long> {
    
    List<MeetingMinutes> findByMeetingPlatform(MeetingMinutes.MeetingPlatform platform);
    
    Optional<MeetingMinutes> findByMeetingIdAndMeetingPlatform(String meetingId, MeetingMinutes.MeetingPlatform platform);
    
    List<MeetingMinutes> findByProcessed(boolean processed);
    
    @Query("SELECT mm FROM MeetingMinutes mm WHERE mm.meetingDate BETWEEN :startDate AND :endDate")
    List<MeetingMinutes> findByMeetingDateBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT mm FROM MeetingMinutes mm JOIN mm.participants p WHERE p = :user")
    List<MeetingMinutes> findByParticipant(@Param("user") User user);
    
    @Query("SELECT mm FROM MeetingMinutes mm JOIN mm.participants p WHERE p = :user AND mm.meetingDate BETWEEN :startDate AND :endDate")
    List<MeetingMinutes> findByParticipantAndDateRange(
        @Param("user") User user, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT mm FROM MeetingMinutes mm WHERE mm.meetingTitle LIKE %:keyword% OR mm.aiSummary LIKE %:keyword%")
    List<MeetingMinutes> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT mm FROM MeetingMinutes mm JOIN mm.participants p WHERE p IN :users AND mm.meetingDate BETWEEN :startDate AND :endDate")
    List<MeetingMinutes> findByParticipantsAndDateRange(
        @Param("users") List<User> users, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT mm FROM MeetingMinutes mm JOIN mm.participants p WHERE p.id = :userId AND mm.meetingDate BETWEEN :startDate AND :endDate")
    List<MeetingMinutes> findByParticipantIdAndDateRange(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT DISTINCT mm FROM MeetingMinutes mm JOIN mm.participants p WHERE p.manager.id = :managerId AND mm.meetingDate BETWEEN :startDate AND :endDate")
    List<MeetingMinutes> findTeamMeetingsByManagerId(
        @Param("managerId") Long managerId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT mm FROM MeetingMinutes mm JOIN mm.participants p WHERE p.id = :userId AND (mm.meetingTitle LIKE %:query% OR mm.aiSummary LIKE %:query% OR mm.keyPoints LIKE %:query%) AND mm.meetingDate BETWEEN :startDate AND :endDate")
    List<MeetingMinutes> searchByParticipantAndContent(
        @Param("userId") Long userId, 
        @Param("query") String query, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
}
