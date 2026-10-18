package com.newtonite.operations.team;

import com.newtonite.operations.common.exception.GlobalExceptionHandler;
import com.newtonite.operations.common.exception.ResourceNotFoundException;
import com.newtonite.operations.common.exception.UnauthorizedOperationException;
import com.newtonite.operations.common.exception.WorkItemConflictException;
import com.newtonite.operations.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/teams - Success returns 201 Created")
    void createTeam_Success() throws Exception {
        String payload = """
                {
                    "name": "DevOps",
                    "description": "Infrastructure engineering"
                }
                """;

        Team team = new Team("DevOps", "Infrastructure engineering");
        when(teamService.createTeam(eq("DevOps"), eq("Infrastructure engineering")))
                .thenReturn(team);

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("DevOps"))
                .andExpect(jsonPath("$.description").value("Infrastructure engineering"));
    }

    @Test
    @DisplayName("POST /api/teams - Blank name returns 400 Bad Request")
    void createTeam_BlankName() throws Exception {
        String payload = """
                {
                    "name": "",
                    "description": "Infrastructure engineering"
                }
                """;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("POST /api/teams - Duplicate name returns 409 Conflict")
    void createTeam_DuplicateName() throws Exception {
        String payload = """
                {
                    "name": "DevOps",
                    "description": "Infrastructure engineering"
                }
                """;

        when(teamService.createTeam(any(), any()))
                .thenThrow(new WorkItemConflictException("Team with name already exists: DevOps"));

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Team with name already exists: DevOps"));
    }

    @Test
    @DisplayName("GET /api/teams/{id} - Success returns 200 OK")
    void getTeam_Success() throws Exception {
        Team team = new Team("DevOps", "Infrastructure engineering");
        when(teamService.getTeam(1L)).thenReturn(team);

        mockMvc.perform(get("/api/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("DevOps"));
    }

    @Test
    @DisplayName("GET /api/teams/{id} - Not found returns 404")
    void getTeam_NotFound() throws Exception {
        when(teamService.getTeam(999L)).thenThrow(new ResourceNotFoundException("Team not found with ID: 999"));

        mockMvc.perform(get("/api/teams/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/members - Success returns 201 Created")
    void addMember_Success() throws Exception {
        String payload = """
                {
                    "userId": 2,
                    "role": "MEMBER"
                }
                """;

        Team team = new Team("DevOps", "Infra");
        User user = new User("Alice", "alice@newtonite.com");
        TeamMembership membership = new TeamMembership(team, user, TeamRole.MEMBER);

        when(teamService.addMember(eq(1L), eq(2L), eq(TeamRole.MEMBER), eq(10L)))
                .thenReturn(membership);

        mockMvc.perform(post("/api/teams/1/members")
                        .param("actingUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/members - Unauthorized non-lead returns 403 Forbidden")
    void addMember_Unauthorized() throws Exception {
        String payload = """
                {
                    "userId": 2,
                    "role": "MEMBER"
                }
                """;

        when(teamService.addMember(eq(1L), eq(2L), eq(TeamRole.MEMBER), eq(10L)))
                .thenThrow(new UnauthorizedOperationException("Only Team Leads can add members to the team"));

        mockMvc.perform(post("/api/teams/1/members")
                        .param("actingUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only Team Leads can add members to the team"));
    }

    @Test
    @DisplayName("DELETE /api/teams/{teamId}/members/{userId} - Success returns 204 No Content")
    void removeMember_Success() throws Exception {
        doNothing().when(teamService).removeMember(1L, 2L, 10L);

        mockMvc.perform(delete("/api/teams/1/members/2")
                        .param("actingUserId", "10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/teams/{teamId}/members/{userId} - Unauthorized returns 403 Forbidden")
    void removeMember_Unauthorized() throws Exception {
        doThrow(new UnauthorizedOperationException("Only Team Leads or the user themselves can remove membership"))
                .when(teamService).removeMember(1L, 2L, 10L);

        mockMvc.perform(delete("/api/teams/1/members/2")
                        .param("actingUserId", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/teams - Success returns list of teams")
    void getAllTeams_Success() throws Exception {
        Team team1 = new Team("DevOps", "Infra");
        Team team2 = new Team("Frontend", "UI");
        when(teamService.getAllTeams()).thenReturn(List.of(team1, team2));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("DevOps"))
                .andExpect(jsonPath("$[1].name").value("Frontend"));
    }

    @Test
    @DisplayName("GET /api/teams/{teamId}/members - Success returns list of team members")
    void getTeamMembers_Success() throws Exception {
        Team team = new Team("DevOps", "Infra");
        User user1 = new User("Alice", "alice@newtonite.com");
        TeamMembership m1 = new TeamMembership(team, user1, TeamRole.TEAM_LEAD);

        when(teamService.getTeamMembers(1L)).thenReturn(List.of(m1));

        mockMvc.perform(get("/api/teams/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Alice"))
                .andExpect(jsonPath("$[0].role").value("TEAM_LEAD"));
    }
}
