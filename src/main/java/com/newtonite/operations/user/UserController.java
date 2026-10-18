package com.newtonite.operations.user;

import com.newtonite.operations.user.dto.CreateUserRequest;
import com.newtonite.operations.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.getDisplayName(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        List<User> users = userService.getAllUsers(activeOnly);
        List<UserResponse> response = users.stream()
                .map(UserResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
