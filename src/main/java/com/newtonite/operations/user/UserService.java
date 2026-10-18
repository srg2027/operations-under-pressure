package com.newtonite.operations.user;

import com.newtonite.operations.common.exception.ResourceNotFoundException;
import com.newtonite.operations.common.exception.WorkItemConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String displayName, String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new WorkItemConflictException("User with email already exists: " + normalizedEmail);
        }

        User user = new User(displayName, normalizedEmail);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers(Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return userRepository.findAllByActiveTrue();
        }
        return userRepository.findAll();
    }
}
