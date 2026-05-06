package com.medical.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client to communicate with users-service (synchronous REST call).
 * Used to validate patient by document number.
 */
@Component
public class PatientClient {

    private static final Logger logger = LoggerFactory.getLogger(PatientClient.class);
    private final RestTemplate restTemplate;
    private final String usersServiceUrl;

    public PatientClient() {
        this.restTemplate = new RestTemplate();
        this.usersServiceUrl = "http://localhost:8081/api/users";
    }

    /**
     * Validate if a patient exists by document number.
     * Calls users-service synchronously via REST.
     */
    public boolean validatePatient(String documentNumber) {
        try {
            String url = usersServiceUrl + "/patients/validate/" + documentNumber;
            logger.info("Validating patient with document: {} via synchronous call to {}", documentNumber, url);

            Boolean response = restTemplate.getForObject(url, Boolean.class);
            logger.info("Validation response for document {}: {}", documentNumber, response);
            return response != null && response;
        } catch (Exception e) {
            logger.error("Error validating patient with document {}: {}", documentNumber, e.getMessage());
            // If users-service is not available, allow creation (for development)
            return true;
        }
    }

    /**
     * Response from users-service.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PatientResponse {
        private Long id;
        private String documentNumber;
        private String firstName;
        private String lastName;
    }
}