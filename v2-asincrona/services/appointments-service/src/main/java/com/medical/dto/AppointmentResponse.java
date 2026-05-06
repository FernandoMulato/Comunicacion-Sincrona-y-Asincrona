package com.medical.dto;

import com.medical.entities.AppointmentStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for appointment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private String patientDocument;
    private String patientName;
    private String patientPhone;
    private Long professionalId;
    private String professionalName;
    private LocalDate date;
    private LocalTime time;
    private Integer durationMinutes;
    private String reason;
    private AppointmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}