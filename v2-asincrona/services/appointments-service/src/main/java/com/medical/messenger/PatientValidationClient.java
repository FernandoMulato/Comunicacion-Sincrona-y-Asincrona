package com.medical.messenger;

import com.medical.config.RabbitMQConfig;
import com.medical.dto.PatientValidationRequest;
import com.medical.dto.PatientValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles async patient validation via RabbitMQ.
 * Sends requests to users-service and waits for responses.
 */
@Component
@Slf4j
public class PatientValidationClient {

    private final RabbitTemplate rabbitTemplate;
    private final Map<String, ResponseHolder> pendingRequests = new ConcurrentHashMap<>();
    private static final int TIMEOUT_SECONDS = 5;

    public PatientValidationClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Validate patient asynchronously via RabbitMQ.
     * Sends request and waits for response with timeout.
     *
     * @param documentNumber The patient document number to validate
     * @return PatientValidationResponse with validation result
     */
    public PatientValidationResponse validatePatient(String documentNumber) {
        String correlationId = UUID.randomUUID().toString();

        PatientValidationRequest request = PatientValidationRequest.builder()
                .correlationId(correlationId)
                .documentNumber(documentNumber)
                .build();

        log.info("Sending validation request for document: {} with correlationId: {}", documentNumber, correlationId);

        // Create holder for response
        ReentrantLock lock = new ReentrantLock();
        Condition responseReady = lock.newCondition();
        ResponseHolder holder = new ResponseHolder(lock, responseReady);
        pendingRequests.put(correlationId, holder);

        try {
            // Send request to queue
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MEDICAL_EXCHANGE,
                    RabbitMQConfig.VALIDATION_REQUEST_ROUTING_KEY,
                    request
            );

            // Wait for response
            lock.lock();
            boolean received = responseReady.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            lock.unlock();

            if (!received) {
                log.warn("Timeout waiting for validation response for correlationId: {}", correlationId);
                pendingRequests.remove(correlationId);
                return PatientValidationResponse.builder()
                        .correlationId(correlationId)
                        .valid(false)
                        .error("Validation service timeout - please try again")
                        .build();
            }

            // Get response
            PatientValidationResponse response = holder.getResponse();
            log.info("Received validation response for correlationId: {}, valid: {}", correlationId, response.isValid());
            pendingRequests.remove(correlationId);
            return response;

        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for validation response", e);
            Thread.currentThread().interrupt();
            pendingRequests.remove(correlationId);
            return PatientValidationResponse.builder()
                    .correlationId(correlationId)
                    .valid(false)
                    .error("Validation interrupted")
                    .build();
        } catch (Exception e) {
            log.error("Error sending validation request: {}", e.getMessage(), e);
            pendingRequests.remove(correlationId);
            return PatientValidationResponse.builder()
                    .correlationId(correlationId)
                    .valid(false)
                    .error("Validation service unavailable: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Listen for validation responses from users-service.
     */
    @RabbitListener(queues = RabbitMQConfig.PATIENT_VALIDATION_RESPONSES_QUEUE)
    public void handleValidationResponse(PatientValidationResponse response) {
        String correlationId = response.getCorrelationId();
        log.info("Received response for correlationId: {}", correlationId);

        ResponseHolder holder = pendingRequests.get(correlationId);
        if (holder != null) {
            holder.getLock().lock();
            try {
                holder.setResponse(response);
                holder.getResponseReady().signal();
            } finally {
                holder.getLock().unlock();
            }
        } else {
            log.warn("No pending request found for correlationId: {}", correlationId);
        }
    }

    /**
     * Helper class to hold response state.
     */
    private static class ResponseHolder {
        private final java.util.concurrent.locks.ReentrantLock lock;
        private final Condition responseReady;
        private PatientValidationResponse response;

        public ResponseHolder(java.util.concurrent.locks.ReentrantLock lock, Condition responseReady) {
            this.lock = lock;
            this.responseReady = responseReady;
        }

        public java.util.concurrent.locks.ReentrantLock getLock() {
            return lock;
        }

        public Condition getResponseReady() {
            return responseReady;
        }

        public PatientValidationResponse getResponse() {
            return response;
        }

        public void setResponse(PatientValidationResponse response) {
            this.response = response;
        }
    }
}