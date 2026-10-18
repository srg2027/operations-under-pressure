package com.newtonite.operations.workitem;

import com.newtonite.operations.common.exception.GlobalExceptionHandler;
import com.newtonite.operations.team.Team;
import com.newtonite.operations.team.TeamMembership;
import com.newtonite.operations.team.TeamMembershipRepository;
import com.newtonite.operations.team.TeamRepository;
import com.newtonite.operations.team.TeamRole;
import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class WorkItemControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WorkItemController workItemController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private WorkItemService workItemService;

    private Team devTeam;
    private User engineer;
    private User lead;
    private User outsider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(workItemController)
                .setControllerAdvice(globalExceptionHandler)
                .build();

        devTeam = teamRepository.save(new Team("DevOps", "Infra delivery"));
        engineer = userRepository.save(new User("Dev Dan", "dan@example.com"));
        lead = userRepository.save(new User("Lead Lisa", "lisa@example.com"));
        outsider = userRepository.save(new User("Outsider Otto", "otto@other.com"));

        teamMembershipRepository.save(new TeamMembership(devTeam, engineer, TeamRole.MEMBER));
        teamMembershipRepository.save(new TeamMembership(devTeam, lead, TeamRole.TEAM_LEAD));
    }

    @Test
    @DisplayName("POST /api/work-items should return 201 CREATED for valid request")
    void shouldCreateWorkItemSuccessfully() throws Exception {
        String payload = """
                {
                    "title": "Fix ingress controller",
                    "description": "High HTTP 502 rate",
                    "priority": "URGENT",
                    "teamId": %d,
                    "creatorId": %d
                }
                """.formatted(devTeam.getId(), engineer.getId());

        mockMvc.perform(post("/api/work-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title", is("Fix ingress controller")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.priority", is("URGENT")))
                .andExpect(jsonPath("$.teamName", is("DevOps")))
                .andExpect(jsonPath("$.creatorDisplayName", is("Dev Dan")))
                .andExpect(jsonPath("$.version", is(0)));
    }

    @Test
    @DisplayName("POST /api/work-items should return 400 BAD_REQUEST on validation failure")
    void shouldRejectInvalidWorkItemPayload() throws Exception {
        String invalid = """
                {
                    "title": "",
                    "description": "desc"
                }
                """;

        mockMvc.perform(post("/api/work-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.title").isNotEmpty())
                .andExpect(jsonPath("$.validationErrors.teamId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/work-items should return 403 FORBIDDEN when creator is not in team")
    void shouldRejectCreationByNonTeamMember() throws Exception {
        String unauthorized = """
                {
                    "title": "Unauthorized Ticket",
                    "description": "Desc",
                    "priority": "LOW",
                    "teamId": %d,
                    "creatorId": %d
                }
                """.formatted(devTeam.getId(), outsider.getId());

        mockMvc.perform(post("/api/work-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unauthorized))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("GET /api/work-items/{id} should return 200 OK or 404 NOT_FOUND")
    void shouldGetWorkItemById() throws Exception {
        WorkItem item = workItemService.createWorkItem("Task to fetch", null, WorkItemPriority.MEDIUM, devTeam.getId(), engineer.getId());

        mockMvc.perform(get("/api/work-items/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(item.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Task to fetch")));

        mockMvc.perform(get("/api/work-items/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("POST /api/work-items/{id}/claim should return 200 OK and 409 CONFLICT on duplicate claim")
    void shouldClaimWorkItemAndPreventDuplicateClaim() throws Exception {
        WorkItem item = workItemService.createWorkItem("Contested task", null, WorkItemPriority.HIGH, devTeam.getId(), engineer.getId());

        mockMvc.perform(post("/api/work-items/{id}/claim", item.getId())
                        .param("userId", engineer.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.assigneeDisplayName", is("Dev Dan")));

        // Attempt second claim -> 409 Conflict
        mockMvc.perform(post("/api/work-items/{id}/claim", item.getId())
                        .param("userId", lead.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("POST /api/work-items/{id}/complete should return 200 OK")
    void shouldCompleteWorkItem() throws Exception {
        WorkItem item = workItemService.createWorkItem("Task to finish", null, WorkItemPriority.LOW, devTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        mockMvc.perform(post("/api/work-items/{id}/complete", item.getId())
                        .param("userId", engineer.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    @DisplayName("PATCH /api/work-items/{id}/priority should update priority")
    void shouldChangePriority() throws Exception {
        WorkItem item = workItemService.createWorkItem("Priority task", null, WorkItemPriority.LOW, devTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        String payload = """
                {
                    "priority": "URGENT",
                    "actorId": %d
                }
                """.formatted(engineer.getId());

        mockMvc.perform(patch("/api/work-items/{id}/priority", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority", is("URGENT")));
    }

    @Test
    @DisplayName("POST /api/work-items/{id}/notes should return 201 CREATED and record in history")
    void shouldAddNoteAndRetrieveHistory() throws Exception {
        WorkItem item = workItemService.createWorkItem("Discussion task", null, WorkItemPriority.MEDIUM, devTeam.getId(), engineer.getId());

        String notePayload = """
                {
                    "note": "Root cause identified in cache layer",
                    "actorId": %d
                }
                """.formatted(lead.getId());

        mockMvc.perform(post("/api/work-items/{id}/notes", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityType", is("NOTE_ADDED")))
                .andExpect(jsonPath("$.details", is("Root cause identified in cache layer")));

        mockMvc.perform(get("/api/work-items/{id}/history", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // CREATED + NOTE_ADDED
                .andExpect(jsonPath("$[0].activityType", is("CREATED")))
                .andExpect(jsonPath("$[1].activityType", is("NOTE_ADDED")));
    }

    @Test
    @DisplayName("POST /api/work-items/{id}/release should release work item back to team queue")
    void shouldReleaseWorkItem() throws Exception {
        WorkItem item = workItemService.createWorkItem("Release task", null, WorkItemPriority.LOW, devTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        mockMvc.perform(post("/api/work-items/{id}/release", item.getId())
                        .param("userId", engineer.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.assigneeDisplayName").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/work-items/{id}/reassign should reassign work item to another team member")
    void shouldReassignWorkItem() throws Exception {
        WorkItem item = workItemService.createWorkItem("Reassign task", null, WorkItemPriority.LOW, devTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        String reassignPayload = """
                {
                    "targetUserId": %d,
                    "actingUserId": %d
                }
                """.formatted(lead.getId(), engineer.getId());

        mockMvc.perform(post("/api/work-items/{id}/reassign", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassignPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.assigneeDisplayName", is("Lead Lisa")));
    }

    @Test
    @DisplayName("GET /api/work-items should list work items by team and optional status filter")
    void shouldListWorkItemsByTeam() throws Exception {
        WorkItem item1 = workItemService.createWorkItem("Task 1", null, WorkItemPriority.LOW, devTeam.getId(), engineer.getId());
        WorkItem item2 = workItemService.createWorkItem("Task 2", null, WorkItemPriority.HIGH, devTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item2.getId(), engineer.getId());

        mockMvc.perform(get("/api/work-items")
                        .param("teamId", devTeam.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/work-items")
                        .param("teamId", devTeam.getId().toString())
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task 1")));
    }

    @Test
    @DisplayName("GET /api/work-items/assigned/{userId} should list work items assigned to user")
    void shouldListWorkItemsAssignedToUser() throws Exception {
        WorkItem item = workItemService.createWorkItem("Assigned task", null, WorkItemPriority.MEDIUM, devTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        mockMvc.perform(get("/api/work-items/assigned/{userId}", engineer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assigneeDisplayName", is("Dev Dan")));
    }
}
