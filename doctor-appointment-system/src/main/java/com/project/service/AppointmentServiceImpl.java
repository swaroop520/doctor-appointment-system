package com.project.service;

import com.project.entity.Appointment;
import com.project.entity.Doctor;
import com.project.entity.User;
import com.project.repository.AppointmentRepository;
import com.project.repository.DoctorRepository;
import com.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Override
    public Appointment bookAppointment(Long patientId, Long doctorId, String date, String time, String notes, String patientName, String patientDetails) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        LocalDate apptDate = LocalDate.parse(date);
        LocalTime apptTime = LocalTime.parse(time);
        appointment.setAppointmentDate(apptDate);
        appointment.setAppointmentTime(apptTime);

        // Auto-approve logic based on availability
        boolean timeConflict = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTime(doctorId,
                apptDate, apptTime);
        if (timeConflict) {
            throw new RuntimeException("The doctor is already booked for this time slot.");
        }

        appointment.setStatus("PENDING"); // Set to PENDING so doctor can approve
        appointment.setType("NORMAL");
        appointment.setNotes(notes);
        appointment.setPatientName(patientName);
        appointment.setPatientDetails(patientDetails);

        return appointmentRepository.save(appointment);
    }

    @Override
    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    @Override
    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    @Override
    public Appointment updateAppointmentStatus(Long appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    @Override
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Override
    public java.time.LocalTime getNextAvailableEmergencySlot() {
        LocalTime startTime = LocalTime.of(16, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        List<Appointment> existingEmergency = appointmentRepository.findByAppointmentDateAndType(LocalDate.now(),
                "EMERGENCY");

        LocalTime allocatedTime = startTime;
        while (allocatedTime.isBefore(endTime)) {
            final LocalTime currentSlot = allocatedTime;
            boolean isTaken = existingEmergency.stream()
                    .anyMatch(a -> a.getAppointmentTime() != null && 
                                   a.getAppointmentTime().equals(currentSlot) &&
                                   !"CANCELLED".equals(a.getStatus()));
            if (!isTaken) {
                return allocatedTime;
            }
            allocatedTime = allocatedTime.plusMinutes(30);
        }
        return null; // No slots available
    }

    @Override
    public Appointment handleEmergency(Long patientId, String patientName, String diseaseDescription, String patientDetails, String transactionId,
            String attachmentUrl, String paymentStatus) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        LocalTime allocatedTime = getNextAvailableEmergencySlot();
        if (allocatedTime == null) {
            throw new RuntimeException("No emergency slots available for today. Please contact the hospital directly.");
        }

        Appointment emergency = new Appointment();
        emergency.setPatient(patient);
        emergency.setAppointmentDate(LocalDate.now());
        emergency.setPatientName(patientName);
        emergency.setDiseaseDescription(diseaseDescription);
        emergency.setPatientDetails(patientDetails);
        emergency.setTransactionId(transactionId);
        emergency.setAttachmentUrl(attachmentUrl);
        emergency.setAppointmentTime(allocatedTime);
        emergency.setStatus("PENDING_VERIFICATION");
        emergency.setType("EMERGENCY");
        emergency.setPaymentStatus(paymentStatus);
        emergency.setAmount(1.0);

        return appointmentRepository.save(emergency);
    }

    @Override
    public Appointment confirmEmergencyPayment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!"EMERGENCY".equals(appointment.getType())) {
            throw new RuntimeException("This is not an emergency appointment.");
        }
        appointment.setStatus("CONFIRMED");
        return appointmentRepository.save(appointment);
    }

    @Override
    public Appointment cancelEmergencyPayment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!"EMERGENCY".equals(appointment.getType())) {
            throw new RuntimeException("This is not an emergency appointment.");
        }
        appointment.setStatus("CANCELLED");
        return appointmentRepository.save(appointment);
    }

    @Override
    public Doctor getDoctorProfileByUserId(Long userId) {
        return doctorRepository.findByUserId(userId).orElse(null);
    }
}
