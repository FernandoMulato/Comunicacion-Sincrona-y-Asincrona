package com.medical.controller;

import com.medical.dto.CreateUserRequest;
import com.medical.dto.UpdateUserRequest;
import com.medical.dto.UserResponse;
import com.medical.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for user management.
 * Following E1-US1, E1-US2, E1-US3, E1-US4 acceptance criteria.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * E1-US3: Get all users (Admin only).
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * E1-US3: Get user by ID (Admin only).
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * E1-US1: Create a new user.
     * POST /api/users
     *
     * Note: For PATIENT role, self-registration is allowed.
     * For ADMIN/SCHEDULER/PROFESSIONAL, admin authorization required.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * E1-US2: Update user.
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * E1-US4: Deactivate user.
     * PATCH /api/users/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * Validate if a patient exists by document number.
     * REST fallback for debug - v2-asincrona uses RabbitMQ async validation.
     * GET /api/users/patients/validate/{documentNumber}
     */
    @GetMapping("/patients/validate/{documentNumber}")
    public ResponseEntity<Boolean> validatePatient(@PathVariable String documentNumber) {
        boolean exists = userService.validatePatientByDocument(documentNumber);
        return ResponseEntity.ok(exists);
    }
}