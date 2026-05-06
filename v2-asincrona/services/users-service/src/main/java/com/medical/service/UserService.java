package com.medical.service;

import com.medical.dto.CreateUserRequest;
import com.medical.dto.UpdateUserRequest;
import com.medical.dto.UserResponse;
import com.medical.entities.User;
import com.medical.entities.UserRole;
import com.medical.entities.Patient;
import com.medical.repository.UserRepository;
import com.medical.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for user management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user.
     * Following E1-US1 acceptance criteria.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Validate username doesn't exist
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("The username already exists");
        }

        // Validate email doesn't exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("The email already exists");
        }

        // Validate password policy (traditional: 8+ chars)
        validatePassword(request.getPassword());

        // For PROFESSIONAL role, must have associated professional (specialty + licenseNumber)
        // Professional data must be created first in professionals-service by admin
        if (request.getRole() == UserRole.PROFESSIONAL) {
            if (request.getSpecialty() == null || request.getLicenseNumber() == null) {
                throw new IllegalArgumentException("A professional must be associated with this user");
            }
        }

        // Create user entity
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // If role is PATIENT, create Patient record
        if (request.getRole() == UserRole.PATIENT) {
            Patient patient = Patient.builder()
                    .user(savedUser)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .documentType(request.getDocumentType())
                    .documentNumber(request.getDocumentNumber())
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .eps(request.getEps())
                    .build();
            patientRepository.save(patient);
        }

        return mapToResponse(savedUser);
    }

    /**
     * Validate password meets minimum policy.
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("The password does not meet the minimum policy");
        }

        // Traditional validation: at least 1 uppercase, 1 number, 1 special char
        boolean hasUppercase = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            if (Character.isDigit(c)) hasNumber = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        if (!hasUppercase || !hasNumber || !hasSpecial) {
            throw new IllegalArgumentException("The password does not meet the minimum policy");
        }
    }

    /**
     * Map user entity to response DTO.
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Get all users (admin only).
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToResponse(user);
    }

    /**
     * Update user.
     * Following E1-US2 acceptance criteria.
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update fields if provided
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("The username already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("The email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null) {
            validatePassword(request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    /**
     * Deactivate user.
     * Following E1-US4 acceptance criteria.
     */
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Cannot deactivate last active admin
        if (user.getRole() == UserRole.ADMIN && user.getActive()) {
            long activeAdmins = userRepository.countByRoleAndActive(UserRole.ADMIN, true);
            if (activeAdmins <= 1) {
                throw new IllegalArgumentException("It is not possible to deactivate the last administrator of the system");
            }
        }

        // Already inactive
        if (!user.getActive()) {
            throw new IllegalArgumentException("The user is already inactive");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Validate if a patient exists by document number.
     * REST fallback for debug - v2-asincrona uses RabbitMQ async validation.
     */
    @Transactional(readOnly = true)
    public boolean validatePatientByDocument(String documentNumber) {
        return patientRepository.existsByDocumentNumber(documentNumber);
    }

    /**
     * Get patient by document number.
     */
    @Transactional(readOnly = true)
    public Optional<Patient> getPatientByDocument(String documentNumber) {
        return patientRepository.findByDocumentNumber(documentNumber);
    }
}