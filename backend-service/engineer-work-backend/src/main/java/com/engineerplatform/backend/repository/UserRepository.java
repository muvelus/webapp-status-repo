package com.engineerplatform.backend.repository;

import com.engineerplatform.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByRole(User.Role role);
    
    List<User> findByManager(User manager);
    
    @Query("SELECT u FROM User u WHERE u.manager = :manager")
    List<User> findDirectReports(@Param("manager") User manager);
    
    @Query("SELECT u FROM User u JOIN u.teams t WHERE t.id = :teamId")
    List<User> findByTeamId(@Param("teamId") Long teamId);
    
    @Query("SELECT u FROM User u WHERE u.role IN ('MANAGER', 'LEADER', 'ADMIN')")
    List<User> findAllManagers();
    
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findAllActiveUsers();
    
    @Query("SELECT u FROM User u WHERE u.githubUsername = :githubUsername")
    Optional<User> findByGithubUsername(@Param("githubUsername") String githubUsername);
    
    @Query("SELECT u FROM User u WHERE u.slackUserId = :slackUserId")
    Optional<User> findBySlackUserId(@Param("slackUserId") String slackUserId);
    
    @Query("SELECT u FROM User u WHERE u.jiraUsername = :jiraUsername")
    Optional<User> findByJiraUsername(@Param("jiraUsername") String jiraUsername);
    
    @Query("SELECT u FROM User u WHERE u.googleEmail = :googleEmail")
    Optional<User> findByGoogleEmail(@Param("googleEmail") String googleEmail);
    
    @Query("SELECT u FROM User u WHERE u.microsoftEmail = :microsoftEmail")
    Optional<User> findByMicrosoftEmail(@Param("microsoftEmail") String microsoftEmail);
}
