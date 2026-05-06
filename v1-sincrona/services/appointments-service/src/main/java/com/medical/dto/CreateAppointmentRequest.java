package com.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating an appointment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentRequest {

    @NotBlank(message = "Patient document is required")
    private String patientDocument;

    @NotBlank(message = "Patient name is required")
    private String patientName;

    private String patientPhone;

    @NotNull(message = "Professional ID is required")
    private Long professionalId;

    @NotBlank(message = "Professional name is required")
    private String professionalName;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Time is required")
    private LocalTime time;

    @Builder.Default
    private Integer durationMinutes = 30;

    @NotBlank(message = "Reason is required")
    private String reason;
}