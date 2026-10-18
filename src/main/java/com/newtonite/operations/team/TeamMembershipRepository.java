package com.newtonite.operations.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

    Optional<TeamMembership> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    List<TeamMembership> findByTeamId(Long teamId);

    List<TeamMembership> findByUserId(Long userId);

    List<TeamMembership> findByTeamIdAndRole(Long teamId, TeamRole role);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);
}
