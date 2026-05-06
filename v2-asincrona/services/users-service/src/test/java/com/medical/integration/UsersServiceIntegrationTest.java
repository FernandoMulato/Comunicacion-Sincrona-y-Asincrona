package com.medical.integration;

import com.medical.dto.CreateUserRequest;
import com.medical.entities.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for users-service.
 * Run with: mvn test -Dtest=UsersServiceIntegrationTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersServiceIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void shouldCreateUserAndPatient() {
    // Create user with PATIENT role
    CreateUserRequest request = CreateUserRequest.builder()
        .username("integration.test")
        .password("Password1!")
        .email("integration@test.com")
        .role(UserRole.PATIENT)
        .firstName("Integration")
        .lastName("Test")
        .documentType("CC")
        .documentNumber("11111111")
        .phone("3001111111")
        .build();

    ResponseEntity<String> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/users",
        request,
        String.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("integration.test"));
  }

  @Test
  void shouldValidatePatientByDocument() {
    // First create a patient
    CreateUserRequest request = CreateUserRequest.builder()
        .username("validate.test")
        .password("Password1!")
        .email("validate@test.com")
        .role(UserRole.PATIENT)
        .firstName("Validate")
        .lastName("Test")
        .documentType("CC")
        .documentNumber("22222222")
        .build();

    restTemplate.postForEntity(
        "http://localhost:" + port + "/api/users",
        request,
        String.class);

    // Then validate the patient exists
    ResponseEntity<Boolean> validationResponse = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/users/patients/validate/22222222",
        Boolean.class);

    assertEquals(HttpStatus.OK, validationResponse.getStatusCode());
    assertTrue(validationResponse.getBody());
  }

  @Test
  void shouldRejectDuplicateUsername() {
    CreateUserRequest request = CreateUserRequest.builder()
        .username("duplicate.user")
        .password("Password1!")
        .email("duplicate@test.com")
        .role(UserRole.PATIENT)
        .build();

    // Create first user
    restTemplate.postForEntity(
        "http://localhost:" + port + "/api/users",
        request,
        String.class);

    // Try to create duplicate
    ResponseEntity<String> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/users",
        request,
        String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("username already exists"));
  }
}