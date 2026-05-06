package com.medical.dto;

import com.medical.entities.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for updating a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    private String username;

    private String password;

    @Email(message = "Invalid email format")
    private String email;

    private UserRole role;

    // Extended fields
    private String firstName;
    private String lastName;
    private String documentType;
    private String documentNumber;
    private String phone;
    private String address;
    private String eps;
    private String specialty;
    private String licenseNumber;
}