package com.medical.messenger;

import com.medical.config.RabbitMQConfig;
import com.medical.dto.PatientValidationRequest;
import com.medical.dto.PatientValidationResponse;
import com.medical.entities.Patient;
import com.medical.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RabbitMQ listener for patient validation requests.
 * Consumes requests from appointments-service and publishes responses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PatientValidationListener {

    private final PatientRepository patientRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Listen for patient validation requests.
     * Validates patient exists in users_db and publishes response.
     */
    @RabbitListener(queues = RabbitMQConfig.PATIENT_VALIDATION_REQUESTS_QUEUE)
    public void handleValidationRequest(PatientValidationRequest request) {
        log.info("Received validation request for document: {}", request.getDocumentNumber());

        try {
            // Validate patient exists in database
            var patientOpt = patientRepository.findByDocumentNumber(request.getDocumentNumber());

            PatientValidationResponse response;
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                response = PatientValidationResponse.builder()
                        .correlationId(request.getCorrelationId())
                        .valid(true)
                        .patientName(patient.getFirstName() + " " + patient.getLastName())
                        .patientDocument(patient.getDocumentNumber())
                        .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build();
                log.info("Patient found: {}", patient.getFirstName());
            } else {
                response = PatientValidationResponse.builder()
                        .correlationId(request.getCorrelationId())
                        .valid(false)
                        .error("Patient not found with document: " + request.getDocumentNumber())
                        .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build();
                log.info("Patient NOT found for document: {}", request.getDocumentNumber());
            }

            // Publish response to response queue
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MEDICAL_EXCHANGE,
                    RabbitMQConfig.VALIDATION_RESPONSE_ROUTING_KEY,
                    response
            );
            log.info("Response published for correlationId: {}", request.getCorrelationId());

        } catch (Exception e) {
            log.error("Error processing validation request: {}", e.getMessage(), e);

            // Publish error response
            PatientValidationResponse errorResponse = PatientValidationResponse.builder()
                    .correlationId(request.getCorrelationId())
                    .valid(false)
                    .error("Validation service error: " + e.getMessage())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MEDICAL_EXCHANGE,
                    RabbitMQConfig.VALIDATION_RESPONSE_ROUTING_KEY,
                    errorResponse
            );
        }
    }
}