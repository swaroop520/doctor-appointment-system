package com.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = true) // Can be null if emergency and not assigned yet
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = true) // Can be null initially until scheduled
    private LocalTime appointmentTime;

    @Column(nullable = false)
    private String status; // e.g., PENDING, CONFIRMED, REJECTED, CANCELLED

    @Column(nullable = false)
    private String type; // e.g., NORMAL, EMERGENCY

    @Column(nullable = true, length = 1000)
    private String notes;

    @Column(nullable = true, length = 2000)
    private String diseaseDescription;

    @Column(nullable = true)
    private String patientName;

    @Column(nullable = true, length = 2000)
    private String patientDetails;

    @Column(nullable = true)
    private String transactionId;

    @Column(nullable = true)
    private String attachmentUrl;

    @Column(nullable = true)
    private String paymentStatus;

    @Column(nullable = true)
    private Double amount;

    @Column(nullable = true)
    private String meetingId;
}
