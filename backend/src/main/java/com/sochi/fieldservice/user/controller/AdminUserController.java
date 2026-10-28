package com.sochi.fieldservice.user.controller;

import com.sochi.fieldservice.user.dto.AdminCreateUserRequest;
import com.sochi.fieldservice.user.dto.UserResponse;
import com.sochi.fieldservice.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return userService.createUser(request);
    }
}
