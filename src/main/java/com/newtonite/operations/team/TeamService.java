package com.newtonite.operations.team;

import com.newtonite.operations.common.exception.ResourceNotFoundException;
import com.newtonite.operations.common.exception.UnauthorizedOperationException;
import com.newtonite.operations.common.exception.WorkItemConflictException;
import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public TeamService(TeamRepository teamRepository,
                       UserRepository userRepository,
                       TeamMembershipRepository teamMembershipRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    public Team createTeam(String name, String description) {
        if (teamRepository.existsByName(name)) {
            throw new WorkItemConflictException("Team with name already exists: " + name);
        }
        Team team = new Team(name, description);
        return teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + teamId));
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public TeamMembership addMember(Long teamId, Long userId, TeamRole role, Long actingUserId) {
        Team team = getTeam(teamId);
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!targetUser.isActive()) {
            throw new UnauthorizedOperationException("Cannot add inactive user to team");
        }

        // Check if there are any existing members in the team
        List<TeamMembership> existingMembers = teamMembershipRepository.findByTeamId(teamId);
        if (!existingMembers.isEmpty()) {
            // Team already has members: acting user must be a Team Lead
            if (actingUserId == null) {
                throw new UnauthorizedOperationException("Acting user ID required to add members to existing team");
            }
            User actingUser = userRepository.findById(actingUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Acting user not found with ID: " + actingUserId));
            if (!actingUser.isActive()) {
                throw new UnauthorizedOperationException("Acting user is inactive");
            }
            Optional<TeamMembership> actingMembership = teamMembershipRepository.findByTeamIdAndUserId(teamId, actingUserId);
            if (actingMembership.isEmpty() || actingMembership.get().getRole() != TeamRole.TEAM_LEAD) {
                throw new UnauthorizedOperationException("Only Team Leads can add members to the team");
            }
        }

        if (teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new WorkItemConflictException("User is already a member of team: " + team.getName());
        }

        TeamMembership membership = new TeamMembership(team, targetUser, role != null ? role : TeamRole.MEMBER);
        return teamMembershipRepository.save(membership);
    }

    public void removeMember(Long teamId, Long userId, Long actingUserId) {
        Team team = getTeam(teamId);

        if (!teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new ResourceNotFoundException("Membership not found for user in team: " + team.getName());
        }

        if (actingUserId == null) {
            throw new UnauthorizedOperationException("Acting user ID required to remove members");
        }

        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Acting user not found with ID: " + actingUserId));

        if (!actingUser.isActive()) {
            throw new UnauthorizedOperationException("Acting user is inactive");
        }

        // User can remove themselves, or a TEAM_LEAD can remove a member
        boolean isSelf = userId.equals(actingUserId);
        if (!isSelf) {
            Optional<TeamMembership> actingMembership = teamMembershipRepository.findByTeamIdAndUserId(teamId, actingUserId);
            if (actingMembership.isEmpty() || actingMembership.get().getRole() != TeamRole.TEAM_LEAD) {
                throw new UnauthorizedOperationException("Only Team Leads or the user themselves can remove membership");
            }
        }

        teamMembershipRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    @Transactional(readOnly = true)
    public List<TeamMembership> getTeamMembers(Long teamId) {
        getTeam(teamId); // ensures team exists
        return teamMembershipRepository.findByTeamId(teamId);
    }
}
