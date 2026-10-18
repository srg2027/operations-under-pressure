package com.newtonite.operations;

import com.newtonite.operations.activity.ActivityLog;
import com.newtonite.operations.activity.ActivityLogRepository;
import com.newtonite.operations.activity.ActivityType;
import com.newtonite.operations.team.Team;
import com.newtonite.operations.team.TeamMembership;
import com.newtonite.operations.team.TeamMembershipRepository;
import com.newtonite.operations.team.TeamRepository;
import com.newtonite.operations.team.TeamRole;
import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
import com.newtonite.operations.workitem.WorkItem;
import com.newtonite.operations.workitem.WorkItemPriority;
import com.newtonite.operations.workitem.WorkItemRepository;
import com.newtonite.operations.workitem.WorkItemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Idempotent demo data loader for the Operations Under Pressure application.
 * Seeds a realistic operational team, team members (including team lead),
 * and representative work items across various lifecycle states.
 *
 * Safe to execute on every application restart: preserves existing data and
 * checks uniqueness before insertion.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    public static final String DEMO_TEAM_NAME = "Site Reliability Engineering";
    public static final String ALICE_EMAIL = "alice.chen@newtonite.com";
    public static final String BOB_EMAIL = "bob.martinez@newtonite.com";
    public static final String CAROL_EMAIL = "carol.danvers@newtonite.com";

    public static final String ITEM_1_TITLE = "Investigate latency spike on payment gateway";
    public static final String ITEM_2_TITLE = "Mitigate Kafka consumer lag on order-events topic";
    public static final String ITEM_3_TITLE = "Renew TLS wild-card certificates on edge ingress";
    public static final String ITEM_4_TITLE = "Optimize database query plans for activity audit log";

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final WorkItemRepository workItemRepository;
    private final ActivityLogRepository activityLogRepository;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    public DataLoader(UserRepository userRepository,
                      TeamRepository teamRepository,
                      TeamMembershipRepository teamMembershipRepository,
                      WorkItemRepository workItemRepository,
                      ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.workItemRepository = workItemRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Demo data seeding is disabled (app.seed.enabled=false).");
            return;
        }
        seedData();
    }

    /**
     * Seeds initial demo users, team, memberships, and work items idempotently.
     */
    @Transactional
    public void seedData() {
        log.info("Checking and seeding demo data for Operations Under Pressure...");

        // 1. Seed Demo Users
        User alice = userRepository.findByEmail(ALICE_EMAIL)
                .orElseGet(() -> {
                    log.info("Seeding demo operator: Alice Chen ({})", ALICE_EMAIL);
                    return userRepository.save(new User("Alice Chen", ALICE_EMAIL));
                });

        User bob = userRepository.findByEmail(BOB_EMAIL)
                .orElseGet(() -> {
                    log.info("Seeding demo operator: Bob Martinez ({})", BOB_EMAIL);
                    return userRepository.save(new User("Bob Martinez", BOB_EMAIL));
                });

        User carol = userRepository.findByEmail(CAROL_EMAIL)
                .orElseGet(() -> {
                    log.info("Seeding demo operator: Carol Danvers ({})", CAROL_EMAIL);
                    return userRepository.save(new User("Carol Danvers", CAROL_EMAIL));
                });

        // 2. Seed Demo Team
        Team sreTeam = teamRepository.findByName(DEMO_TEAM_NAME)
                .orElseGet(() -> {
                    log.info("Seeding demo operational team: {}", DEMO_TEAM_NAME);
                    return teamRepository.save(new Team(
                            DEMO_TEAM_NAME,
                            "Platform observability, incident response, and mission-critical production operations."
                    ));
                });

        // 3. Seed Team Memberships
        if (!teamMembershipRepository.existsByTeamIdAndUserId(sreTeam.getId(), alice.getId())) {
            log.info("Assigning {} as TEAM_LEAD in {}", alice.getDisplayName(), sreTeam.getName());
            teamMembershipRepository.save(new TeamMembership(sreTeam, alice, TeamRole.TEAM_LEAD));
        }

        if (!teamMembershipRepository.existsByTeamIdAndUserId(sreTeam.getId(), bob.getId())) {
            log.info("Assigning {} as MEMBER in {}", bob.getDisplayName(), sreTeam.getName());
            teamMembershipRepository.save(new TeamMembership(sreTeam, bob, TeamRole.MEMBER));
        }

        if (!teamMembershipRepository.existsByTeamIdAndUserId(sreTeam.getId(), carol.getId())) {
            log.info("Assigning {} as MEMBER in {}", carol.getDisplayName(), sreTeam.getName());
            teamMembershipRepository.save(new TeamMembership(sreTeam, carol, TeamRole.MEMBER));
        }

        // 4. Seed Representative Work Items
        List<WorkItem> existingItems = workItemRepository.findByTeamId(sreTeam.getId());

        // Item 1: IN_PROGRESS assigned to Bob (High Priority)
        if (existingItems.stream().noneMatch(w -> w.getTitle().equals(ITEM_1_TITLE))) {
            log.info("Seeding work item: {}", ITEM_1_TITLE);
            WorkItem item1 = new WorkItem(
                    ITEM_1_TITLE,
                    "P99 latency degraded from 80ms to 420ms following deployment v2.4.1. Error budget at risk.",
                    WorkItemPriority.HIGH,
                    sreTeam,
                    alice
            );
            item1.claim(bob);
            WorkItem saved1 = workItemRepository.save(item1);

            activityLogRepository.save(new ActivityLog(
                    saved1, alice, ActivityType.CREATED, "Work item created in team " + sreTeam.getName()
            ));
            activityLogRepository.save(new ActivityLog(
                    saved1, bob, ActivityType.CLAIMED, "Claimed by " + bob.getDisplayName()
            ));
            activityLogRepository.save(new ActivityLog(
                    saved1, bob, ActivityType.NOTE_ADDED,
                    "Tracing indicates connection pool starvation under peak load. Investigating database proxy settings."
            ));
        }

        // Item 2: OPEN Unassigned Urgent (Urgent Priority - displayed on Urgent Dashboard panel)
        if (existingItems.stream().noneMatch(w -> w.getTitle().equals(ITEM_2_TITLE))) {
            log.info("Seeding work item: {}", ITEM_2_TITLE);
            WorkItem item2 = new WorkItem(
                    ITEM_2_TITLE,
                    "Consumer partition lag exceeded 50,000 messages. Worker thread blocked on external lock.",
                    WorkItemPriority.URGENT,
                    sreTeam,
                    alice
            );
            WorkItem saved2 = workItemRepository.save(item2);

            activityLogRepository.save(new ActivityLog(
                    saved2, alice, ActivityType.CREATED, "Work item created in team " + sreTeam.getName()
            ));
        }

        // Item 3: DONE resolved by Alice (Medium Priority)
        if (existingItems.stream().noneMatch(w -> w.getTitle().equals(ITEM_3_TITLE))) {
            log.info("Seeding work item: {}", ITEM_3_TITLE);
            WorkItem item3 = new WorkItem(
                    ITEM_3_TITLE,
                    "Annual ingress cert rotation completed and validated across staging and production clusters.",
                    WorkItemPriority.MEDIUM,
                    sreTeam,
                    bob
            );
            item3.claim(alice);
            item3.complete();
            WorkItem saved3 = workItemRepository.save(item3);

            activityLogRepository.save(new ActivityLog(
                    saved3, bob, ActivityType.CREATED, "Work item created in team " + sreTeam.getName()
            ));
            activityLogRepository.save(new ActivityLog(
                    saved3, alice, ActivityType.CLAIMED, "Claimed by " + alice.getDisplayName()
            ));
            activityLogRepository.save(new ActivityLog(
                    saved3, alice, ActivityType.COMPLETED,
                    "Cert-manager ingress secrets refreshed and verified across edge proxies with zero downtime."
            ));
        }

        // Item 4: OPEN Backlog (Low Priority)
        if (existingItems.stream().noneMatch(w -> w.getTitle().equals(ITEM_4_TITLE))) {
            log.info("Seeding work item: {}", ITEM_4_TITLE);
            WorkItem item4 = new WorkItem(
                    ITEM_4_TITLE,
                    "Review composite index performance on (work_item_id, created_at) under sustained write load.",
                    WorkItemPriority.LOW,
                    sreTeam,
                    carol
            );
            WorkItem saved4 = workItemRepository.save(item4);

            activityLogRepository.save(new ActivityLog(
                    saved4, carol, ActivityType.CREATED, "Work item created in team " + sreTeam.getName()
            ));
        }

        log.info("Demo data seeding completed successfully.");
    }
}
