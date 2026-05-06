package com.medical.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Professional (medical staff) specific data.
 * Schedule management is handled by professionals-service.
 */
@Entity
@Table(name = "professionals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Professional {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "first_name", nullable = false, length = 50)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 50)
  private String lastName;

  @Column(nullable = false, length = 50)
  private String specialty; // Neuralterapia, Quiropraxia, Fisioterapia

  @Column(name = "license_number", nullable = false, unique = true, length = 30)
  private String licenseNumber;

  @Column(length = 20)
  private String phone;
}