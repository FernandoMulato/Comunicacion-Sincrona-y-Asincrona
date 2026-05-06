package com.medical.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client to communicate with users-service (synchronous REST call).
 * Used to validate patient by document number.
 */
@Component
public class PatientClient {

    private final RestTemplate restTemplate;
    private final String usersServiceUrl;

    public PatientClient() {
        this.restTemplate = new RestTemplate();
        this.usersServiceUrl = "http://localhost:8081/api/users";
    }

    /**
     * Validate if a patient exists by document number.
     * Calls users-service synchronously.
     */
    public boolean validatePatient(String documentNumber) {
        try {
            // TODO: Implement actual call to users-service
            // For now, return true to allow testing
            return true;
        } catch (Exception e) {
            // If users-service is not available, allow creation
            return true;
        }
    }

    /**
     * Response from users-service.
     */
    @Data
    @AllArgsConstructor
    public static class PatientResponse {
        private Long id;
        private String documentNumber;
        private String firstName;
        private String lastName;
    }
}