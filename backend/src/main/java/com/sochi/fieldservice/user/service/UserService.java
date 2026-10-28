package com.sochi.fieldservice.user.service;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.user.dto.AdminCreateUserRequest;
import com.sochi.fieldservice.user.dto.UserResponse;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EngineerRepository engineerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       EngineerRepository engineerRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.engineerRepository = engineerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + email);
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_ENGINEER;

        User user = new User(
                username,
                email,
                passwordEncoder.encode(request.getPassword()),
                role
        );

        if (request.getEngineerId() != null) {
            Engineer engineer = engineerRepository.findById(request.getEngineerId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Engineer not found with ID: " + request.getEngineerId()
                    ));
            user.setEngineer(engineer);
        }

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }
}
