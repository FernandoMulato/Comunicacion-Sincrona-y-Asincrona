package com.medical.integration;

import com.medical.dto.CreateAppointmentRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for appointments-service with async RabbitMQ.
 * 
 * NOTE: Requires services to be running manually:
 * 1. docker-compose up -d (RabbitMQ + PostgreSQL)
 * 2. java -jar services/users-service/target/users-service-1.0.0-SNAPSHOT.jar
 * 3. java -jar services/appointments-service/target/appointments-service-1.0.0-SNAPSHOT.jar
 * 4. Then run: mvn test -Dtest=AppointmentsServiceIntegrationTest
 * 
 * Or better: Run manually via curl and verify the flow works.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Requires manual service startup - see class Javadoc")
class AppointmentsServiceIntegrationTest {

    private static final String USERS_SERVICE_URL = "http://localhost:8081";
    private static final String APPOINTMENTS_SERVICE_URL = "http://localhost:8082";
    private static final String TEST_USER_DOC = "55555555";

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // Create test patient in users-service if needed
    }

    @Test
    void shouldReturnServiceHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                APPOINTMENTS_SERVICE_URL + "/actuator/health",
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldCreateAppointment_whenPatientExists() {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument(TEST_USER_DOC)
                .patientName("Test Patient")
                .patientPhone("3005555555")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().plusDays(5))
                .time(LocalTime.of(10, 0))
                .durationMinutes(30)
                .reason("Integration test")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                APPOINTMENTS_SERVICE_URL + "/api/appointments",
                request,
                String.class
        );

        // Verify async flow works
        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                "Response: " + response.getBody());
    }

    @Test
    void shouldReject_whenPatientNotExists() {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("NONEXISTENT999")
                .patientName("Test")
                .patientPhone("3000000000")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().plusDays(5))
                .time(LocalTime.of(11, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                APPOINTMENTS_SERVICE_URL + "/api/appointments",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReject_whenDateInPast() {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument(TEST_USER_DOC)
                .patientName("Test")
                .patientPhone("3000000000")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().minusDays(1))
                .time(LocalTime.of(10, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                APPOINTMENTS_SERVICE_URL + "/api/appointments",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toLowerCase().contains("past"));
    }

    @Test
    void shouldGetAllAppointments() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                APPOINTMENTS_SERVICE_URL + "/api/appointments",
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}