package com.medical.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Patient specific data.
 */
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

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

  @Column(name = "document_type", nullable = false, length = 10)
  private String documentType; // CC, TI, RC, CE, etc.

  @Column(name = "document_number", nullable = false, unique = true, length = 20)
  private String documentNumber;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(length = 20)
  private String phone;

  @Column(length = 255)
  private String address;

  @Column(length = 100)
  private String eps;
}