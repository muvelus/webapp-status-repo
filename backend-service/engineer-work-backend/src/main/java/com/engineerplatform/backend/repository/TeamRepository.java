package com.engineerplatform.backend.repository;

import com.engineerplatform.backend.model.Team;
import com.engineerplatform.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    Optional<Team> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Team> findByTeamLead(User teamLead);
    
    @Query("SELECT t FROM Team t JOIN t.members m WHERE m = :user")
    List<Team> findByMember(@Param("user") User user);
    
    @Query("SELECT t FROM Team t WHERE t.teamLead = :teamLead OR :teamLead MEMBER OF t.members")
    List<Team> findByTeamLeadOrMember(@Param("teamLead") User teamLead);
    
    @Query("SELECT COUNT(m) FROM Team t JOIN t.members m WHERE t.id = :teamId")
    Long countMembersByTeamId(@Param("teamId") Long teamId);
}
