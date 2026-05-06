package com.medical.entities;

/**
 * Appointment status enum.
 */
public enum AppointmentStatus {
    SCHEDULED,    // Cita agendada
    CONFIRMED,    // Cita confirmada
    IN_PROGRESS,  // En progreso (el paciente está siendo atendido)
    COMPLETED,    // Cita completada
    CANCELLED,    // Cita cancelada
    NO_SHOW       // Paciente no asistió
}