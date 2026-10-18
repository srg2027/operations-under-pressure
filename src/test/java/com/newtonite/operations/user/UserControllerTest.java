package com.newtonite.operations.user;

import com.newtonite.operations.common.exception.GlobalExceptionHandler;
import com.newtonite.operations.common.exception.ResourceNotFoundException;
import com.newtonite.operations.common.exception.WorkItemConflictException;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/users - Success returns 201 Created")
    void createUser_Success() throws Exception {
        String payload = """
                {
                    "displayName": "Alice Engineer",
                    "email": "alice@newtonite.com"
                }
                """;
        User savedUser = new User("Alice Engineer", "alice@newtonite.com");

        when(userService.createUser(eq("Alice Engineer"), eq("alice@newtonite.com")))
                .thenReturn(savedUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Alice Engineer"))
                .andExpect(jsonPath("$.email").value("alice@newtonite.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/users - Invalid email returns 400 Bad Request")
    void createUser_InvalidEmail() throws Exception {
        String payload = """
                {
                    "displayName": "Alice Engineer",
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/users - Blank name returns 400 Bad Request")
    void createUser_BlankDisplayName() throws Exception {
        String payload = """
                {
                    "displayName": "",
                    "email": "alice@newtonite.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.displayName").exists());
    }

    @Test
    @DisplayName("POST /api/users - Duplicate email returns 409 Conflict")
    void createUser_DuplicateEmail() throws Exception {
        String payload = """
                {
                    "displayName": "Alice Engineer",
                    "email": "alice@newtonite.com"
                }
                """;

        when(userService.createUser(any(), any()))
                .thenThrow(new WorkItemConflictException("User with email already exists: alice@newtonite.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User with email already exists: alice@newtonite.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Success returns 200 OK")
    void getUser_Success() throws Exception {
        User user = new User("Alice Engineer", "alice@newtonite.com");
        when(userService.getUser(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Engineer"))
                .andExpect(jsonPath("$.email").value("alice@newtonite.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Missing user returns 404 Not Found")
    void getUser_NotFound() throws Exception {
        when(userService.getUser(999L)).thenThrow(new ResourceNotFoundException("User not found with ID: 999"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/users - Success lists users")
    void getAllUsers_Success() throws Exception {
        User user1 = new User("Alice", "alice@newtonite.com");
        User user2 = new User("Bob", "bob@newtonite.com");
        when(userService.getAllUsers(anyBoolean())).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].displayName").value("Alice"))
                .andExpect(jsonPath("$[1].displayName").value("Bob"));
    }
}
