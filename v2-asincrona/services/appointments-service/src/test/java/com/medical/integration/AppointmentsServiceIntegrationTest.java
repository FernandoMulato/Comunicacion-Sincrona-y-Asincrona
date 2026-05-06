package com.medical.integration;

import com.medical.dto.CreateAppointmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for appointments-service with synchronous communication.
 * Run with: mvn test -Dtest=AppointmentsServiceIntegrationTest
 * 
 * Note: Requires users-service running on port 8081
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppointmentsServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAppointment_whenPatientExists() {
        // Create appointment with existing patient document (55555555 was created earlier)
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("55555555")
                .patientName("Test Patient")
                .patientPhone("3005555555")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().plusDays(5))
                .time(LocalTime.of(10, 0))
                .durationMinutes(30)
                .reason("Integration test appointment")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/appointments",
                request,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().contains("55555555"));
    }

    @Test
    void shouldRejectAppointment_whenPatientNotExists() {
        // Try to create appointment with non-existing patient
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("99999999")  // Does not exist in patients table
                .patientName("Non-existent Patient")
                .patientPhone("3009999999")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().plusDays(5))
                .time(LocalTime.of(11, 0))
                .durationMinutes(30)
                .reason("Should fail")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/appointments",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Patient not found"));
    }

    @Test
    void shouldRejectAppointment_whenDateInPast() {
        // Create appointment with past date
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("55555555")
                .patientName("Test Patient")
                .patientPhone("3005555555")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().minusDays(1))  // Past date
                .time(LocalTime.of(10, 0))
                .durationMinutes(30)
                .reason("Should fail")
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
}