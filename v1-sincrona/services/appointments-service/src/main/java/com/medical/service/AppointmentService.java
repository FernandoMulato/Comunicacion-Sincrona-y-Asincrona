package com.medical.service;

import com.medical.client.PatientClient;
import com.medical.dto.AppointmentResponse;
import com.medical.dto.CreateAppointmentRequest;
import com.medical.dto.UpdateAppointmentRequest;
import com.medical.entities.Appointment;
import com.medical.entities.AppointmentStatus;
import com.medical.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for appointment management.
 * Following E3-US1, E3-US2, E3-US3, E4-US1, E4-US2 acceptance criteria.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientClient patientClient;

    /**
     * Create a new appointment.
     * Following E3-US1 (Patient) and E4-US1 (Scheduler) scenarios.
     */
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        // Validate date is not in the past (E3-US3)
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot schedule appointments in the past");
        }

        // Validate time slot availability (E3-US2 - concurrency collision)
        appointmentRepository.findConflictingAppointment(
                request.getProfessionalId(),
                request.getDate(),
                request.getTime()
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("Time slot not available");
        });

        // Validate professional availability with professionals-service
        // TODO: Call professionals-service to check availability

        // Create appointment entity
        Appointment appointment = Appointment.builder()
                .patientDocument(request.getPatientDocument())
                .patientName(request.getPatientName())
                .patientPhone(request.getPatientPhone())
                .professionalId(request.getProfessionalId())
                .professionalName(request.getProfessionalName())
                .date(request.getDate())
                .time(request.getTime())
                .durationMinutes(request.getDurationMinutes() != null ?
                        request.getDurationMinutes() : 30)
                .reason(request.getReason())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    /**
     * Get all appointments.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAllByOrderByDateDescTimeDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get appointment by ID.
     */
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        return mapToResponse(appointment);
    }

    /**
     * Get appointments by patient document.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(String documentNumber) {
        return appointmentRepository.findByPatientDocumentOrderByDateDescTimeDesc(documentNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Cancel an appointment.
     * Following E3-US3.
     */
    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Cannot cancel already cancelled or completed
        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new IllegalArgumentException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    /**
     * Update an appointment (reschedule).
     * Following E4-US2.
     */
    @Transactional
    public AppointmentResponse updateAppointment(Long id, UpdateAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Can only update scheduled or confirmed appointments
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED &&
            appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot update this appointment");
        }

        // If changing date/time, check availability
        LocalDate newDate = request.getDate() != null ? request.getDate() : appointment.getDate();
        var newTime = request.getTime() != null ? request.getTime() : appointment.getTime();

        if (request.getDate() != null || request.getTime() != null) {
            appointmentRepository.findConflictingAppointment(
                    appointment.getProfessionalId(),
                    newDate,
                    newTime
            ).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Time slot not available");
                }
            });
        }

        // Update fields
        if (request.getDate() != null) {
            if (request.getDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Cannot reschedule to a past date");
            }
            appointment.setDate(request.getDate());
        }
        if (request.getTime() != null) {
            appointment.setTime(request.getTime());
        }
        if (request.getDurationMinutes() != null) {
            appointment.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getReason() != null) {
            appointment.setReason(request.getReason());
        }
        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    /**
     * Map entity to response DTO.
     */
    private AppointmentResponse mapToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientDocument(appointment.getPatientDocument())
                .patientName(appointment.getPatientName())
                .patientPhone(appointment.getPatientPhone())
                .professionalId(appointment.getProfessionalId())
                .professionalName(appointment.getProfessionalName())
                .date(appointment.getDate())
                .time(appointment.getTime())
                .durationMinutes(appointment.getDurationMinutes())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}