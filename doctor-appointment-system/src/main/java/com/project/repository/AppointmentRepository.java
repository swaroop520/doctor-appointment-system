package com.project.repository;

import com.project.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByType(String type);

    List<Appointment> findByAppointmentDateAndType(java.time.LocalDate appointmentDate, String type);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(Long doctorId, java.time.LocalDate date,
            java.time.LocalTime time);
}
