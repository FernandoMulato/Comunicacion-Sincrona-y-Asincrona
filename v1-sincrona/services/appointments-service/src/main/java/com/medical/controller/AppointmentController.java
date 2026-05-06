package com.medical.controller;

import com.medical.dto.AppointmentResponse;
import com.medical.dto.CreateAppointmentRequest;
import com.medical.dto.UpdateAppointmentRequest;
import com.medical.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for appointment management.
 * Following E3-US1, E3-US2, E3-US3, E4-US1, E4-US2 acceptance criteria.
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Get all appointments.
     * GET /api/appointments
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    /**
     * Get appointment by ID.
     * GET /api/appointments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    /**
     * Get appointments by patient document.
     * GET /api/appointments/patient/{document}
     */
    @GetMapping("/patient/{document}")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByPatient(@PathVariable String document) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(document));
    }

    /**
     * Create a new appointment.
     * POST /api/appointments
     *
     * Used by patients (E3-US1) and schedulers (E4-US1)
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an appointment (reschedule).
     * PUT /api/appointments/{id}
     *
     * Following E4-US2
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, request));
    }

    /**
     * Cancel an appointment.
     * PATCH /api/appointments/{id}/cancel
     *
     * Following E3-US3
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}