package com.medical.repository;

import com.medical.entities.Appointment;
import com.medical.entities.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Appointment entity.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  /**
   * Find appointments by professional and date (to check availability).
   */
  List<Appointment> findByProfessionalIdAndDate(Long professionalId, LocalDate date);

  /**
   * Find appointments by professional, date, and time.
   */
  @Query("SELECT a FROM Appointment a WHERE a.professionalId = :professionalId " +
      "AND a.date = :date AND a.time = :time " +
      "AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
  Optional<Appointment> findConflictingAppointment(
      @Param("professionalId") Long professionalId,
      @Param("date") LocalDate date,
      @Param("time") LocalTime time);

  /**
   * Find appointments by patient document.
   */
  List<Appointment> findByPatientDocumentOrderByDateDescTimeDesc(String patientDocument);

  /**
   * Find appointments by status.
   */
  List<Appointment> findByStatus(AppointmentStatus status);

  /**
   * Find all appointments ordered by date and time.
   */
  List<Appointment> findAllByOrderByDateDescTimeDesc();
}