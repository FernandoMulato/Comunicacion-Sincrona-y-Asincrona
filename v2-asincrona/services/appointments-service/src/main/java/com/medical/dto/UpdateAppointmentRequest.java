package com.medical.dto;

import com.medical.entities.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for updating an appointment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAppointmentRequest {

    private LocalDate date;
    private LocalTime time;
    private Integer durationMinutes;
    private String reason;
    private AppointmentStatus status;
}