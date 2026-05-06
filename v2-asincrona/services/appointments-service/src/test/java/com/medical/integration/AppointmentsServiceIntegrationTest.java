package com.medical.integration;

import com.medical.dto.CreateAppointmentRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for appointments-service with async RabbitMQ communication.
 * Uses TestContainers to spin up RabbitMQ and PostgreSQL.
 * 
 * Run with: mvn test -Dtest=AppointmentsServiceIntegrationTest
 * Requires: Docker running and accessible
 * 
 * Note: These tests require Docker to be available. If Docker is not available,
 * the tests will be skipped.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Requires Docker - run manually with services up or use TestContainers in CI/CD")
class AppointmentsServiceIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management");

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("appointments_db")
            .withUsername("medical_user")
            .withPassword("medical123");

    @BeforeAll
    static void checkDocker() {
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Docker not available - skipping integration tests");
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRejectAppointment_whenDateInPast() {
        // Test that past dates are rejected (doesn't need users-service)
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("55555555")
                .patientName("Test Patient")
                .patientPhone("3005555555")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().minusDays(1))
                .time(LocalTime.of(10, 0))
                .durationMinutes(30)
                .reason("Should fail - past date")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/appointments",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("past"));
    }

    @Test
    void shouldGetAllAppointments() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/appointments",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().startsWith("["));
    }

    @Test
    void shouldReturnServiceHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }
}