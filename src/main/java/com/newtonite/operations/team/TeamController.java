package com.newtonite.operations.team;

import com.newtonite.operations.team.dto.AddMemberRequest;
import com.newtonite.operations.team.dto.CreateTeamRequest;
import com.newtonite.operations.team.dto.TeamMemberResponse;
import com.newtonite.operations.team.dto.TeamResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        Team created = teamService.createTeam(request.getName(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.fromEntity(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable Long id) {
        Team team = teamService.getTeam(id);
        return ResponseEntity.ok(TeamResponse.fromEntity(team));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        List<Team> teams = teamService.getAllTeams();
        List<TeamResponse> response = teams.stream()
                .map(TeamResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddMemberRequest request,
            @RequestParam(required = false) Long actingUserId
    ) {
        TeamMembership membership = teamService.addMember(teamId, request.getUserId(), request.getRole(), actingUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamMemberResponse.fromEntity(membership));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @RequestParam Long actingUserId
    ) {
        teamService.removeMember(teamId, userId, actingUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable Long teamId) {
        List<TeamMembership> members = teamService.getTeamMembers(teamId);
        List<TeamMemberResponse> response = members.stream()
                .map(TeamMemberResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
