package com.medical.dto;

import com.medical.entities.UserRole;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for user data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Patient/Professional extended info
    private String firstName;
    private String lastName;
    private String specialty;  // Only for PROFESSIONAL
    private String licenseNumber;  // Only for PROFESSIONAL
}