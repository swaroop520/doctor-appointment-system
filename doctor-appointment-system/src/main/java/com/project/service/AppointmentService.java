package com.project.service;

import com.project.entity.Appointment;
import com.project.entity.Doctor;
import java.util.List;

public interface AppointmentService {
    Appointment bookAppointment(Long patientId, Long doctorId, String date, String time, String notes, String patientName, String patientDetails);

    List<Appointment> getPatientAppointments(Long patientId);

    List<Appointment> getDoctorAppointments(Long doctorId);

    Appointment updateAppointmentStatus(Long appointmentId, String status);

    List<Appointment> getAllAppointments();

    Appointment handleEmergency(Long patientId, String patientName, String diseaseDescription, String patientDetails, String transactionId, String attachmentUrl, String paymentStatus);

    java.time.LocalTime getNextAvailableEmergencySlot();

    Appointment confirmEmergencyPayment(Long id);
    
    Appointment cancelEmergencyPayment(Long id);

    Doctor getDoctorProfileByUserId(Long userId);
}
