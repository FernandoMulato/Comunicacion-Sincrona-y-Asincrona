package com.medical.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Request message for patient validation via RabbitMQ.
 * Sent from appointments-service to users-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientValidationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique correlation ID to match request with response.
     */
    private String correlationId;

    /**
     * Document number to validate.
     */
    private String documentNumber;

    /**
     * Timestamp when request was created.
     */
    private LocalDateTime timestamp;
}