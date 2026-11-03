package com.sochi.fieldservice.security;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.security.dto.AuthResponse;
import com.sochi.fieldservice.security.dto.ForgotPasswordRequest;
import com.sochi.fieldservice.security.dto.ForgotPasswordResponse;
import com.sochi.fieldservice.security.dto.LoginRequest;
import com.sochi.fieldservice.security.dto.MessageResponse;
import com.sochi.fieldservice.security.dto.RegisterRequest;
import com.sochi.fieldservice.security.dto.ResetPasswordRequest;
import com.sochi.fieldservice.security.jwt.JwtUtils;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EngineerRepository engineerRepository;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            @Autowired(required = false) EngineerRepository engineerRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.engineerRepository = engineerRepository;
    }

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils) {
        this(userRepository, passwordEncoder, authenticationManager, jwtUtils, null);
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + email);
        }

        Engineer savedEngineer = null;
        if (engineerRepository != null) {
            Engineer engineer = new Engineer();
            engineer.setName(username);
            engineer.setEmail(email);
            engineer.setSpecialization("Field Operations");
            engineer.setPhone("+91-98000-00000");
            engineer.setActive(true);
            engineer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            savedEngineer = engineerRepository.save(engineer);
        }

        User user = new User(
                username,
                email,
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_ENGINEER,
                savedEngineer
        );

        User savedUser = userRepository.save(user);
        String token = jwtUtils.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername().trim();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String token = jwtUtils.generateToken(user);

            return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getUsernameOrEmail().trim();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No account found with username or email: " + identifier
                ));

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        return new ForgotPasswordResponse(
                "Password reset code generated. Use this code to reset your password within 15 minutes.",
                token
        );
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String token = request.getResetToken().trim().toUpperCase();
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or expired password reset token"
                ));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password reset token has expired. Please request a new one."
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return new MessageResponse("Password has been reset successfully. You can now sign in.");
    }
}
