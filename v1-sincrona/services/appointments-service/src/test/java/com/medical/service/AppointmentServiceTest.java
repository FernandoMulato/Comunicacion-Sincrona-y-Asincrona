package com.medical.service;

import com.medical.client.PatientClient;
import com.medical.dto.AppointmentResponse;
import com.medical.dto.CreateAppointmentRequest;
import com.medical.entities.Appointment;
import com.medical.entities.AppointmentStatus;
import com.medical.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for AppointmentService - TDD (RED → GREEN → REFACTOR)
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientClient patientClient;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(appointmentRepository, patientClient);
    }

    @Test
    void shouldCreateAppointment_whenValidData() {
        // Given
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("12345678")
                .patientName("Juan Perez")
                .patientPhone("3001234567")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().plusDays(1))  // Tomorrow
                .time(LocalTime.of(9, 0))
                .durationMinutes(30)
                .reason("Dolor de cabeza")
                .build();

        when(appointmentRepository.findConflictingAppointment(1L,
                LocalDate.now().plusDays(1), LocalTime.of(9, 0)))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        // When
        AppointmentResponse response = appointmentService.createAppointment(request);

        // Then
        assertNotNull(response);
        assertEquals("12345678", response.getPatientDocument());
        assertEquals("Juan Perez", response.getPatientName());
        assertEquals(AppointmentStatus.SCHEDULED, response.getStatus());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void shouldThrowException_whenTimeSlotNotAvailable() {
        // Given - Another patient already booked this time slot
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("87654321")
                .patientName("Maria Garcia")
                .patientPhone("3009876543")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().plusDays(1))
                .time(LocalTime.of(9, 0))  // Same time as existing
                .durationMinutes(30)
                .reason("Chequeo general")
                .build();

        // There's already an appointment at this time
        Appointment existing = Appointment.builder()
                .id(1L)
                .patientDocument("12345678")
                .professionalId(1L)
                .date(LocalDate.now().plusDays(1))
                .time(LocalTime.of(9, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findConflictingAppointment(1L,
                LocalDate.now().plusDays(1), LocalTime.of(9, 0)))
                .thenReturn(Optional.of(existing));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldThrowException_whenDateIsPast() {
        // Given
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientDocument("12345678")
                .patientName("Juan Perez")
                .patientPhone("3001234567")
                .professionalId(1L)
                .professionalName("Dr. Smith")
                .date(LocalDate.now().minusDays(1))  // Past date
                .time(LocalTime.of(9, 0))
                .durationMinutes(30)
                .reason("Dolor de cabeza")
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request));
    }

    @Test
    void shouldCancelAppointment_whenExistsAndScheduled() {
        // Given
        Appointment appointment = Appointment.builder()
                .id(1L)
                .patientDocument("12345678")
                .professionalId(1L)
                .date(LocalDate.now().plusDays(1))
                .time(LocalTime.of(9, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        // When
        appointmentService.cancelAppointment(1L);

        // Then
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void shouldThrowException_whenCancelAlreadyCancelled() {
        // Given - Already cancelled
        Appointment appointment = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.cancelAppointment(1L));
    }

    @Test
    void shouldReturnAllAppointments() {
        // Given
        Appointment a1 = Appointment.builder()
                .id(1L)
                .patientDocument("12345678")
                .date(LocalDate.now().plusDays(1))
                .build();
        Appointment a2 = Appointment.builder()
                .id(2L)
                .patientDocument("87654321")
                .date(LocalDate.now().plusDays(2))
                .build();

        when(appointmentRepository.findAllByOrderByDateDescTimeDesc())
                .thenReturn(java.util.List.of(a1, a2));

        // When
        var appointments = appointmentService.getAllAppointments();

        // Then
        assertEquals(2, appointments.size());
    }
}