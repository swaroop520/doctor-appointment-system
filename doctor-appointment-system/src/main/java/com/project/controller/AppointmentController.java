package com.project.controller;

import com.project.entity.Appointment;
import com.project.security.UserDetailsImpl;
import com.project.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // PATIENT: Book Appointment
    @PostMapping("/book")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> bookAppointment(@RequestBody Map<String, String> request) {
        Long doctorId = Long.parseLong(request.get("doctorId"));
        String date = request.get("date");
        String time = request.get("time");
        String notes = request.get("notes");

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Appointment appointment = appointmentService.bookAppointment(userDetails.getId(), doctorId, date, time, notes);

        return ResponseEntity.ok(appointment);
    }

    // PATIENT: Report Emergency
    @PostMapping(value = "/emergency", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> reportEmergency(
            @RequestPart("patientName") String patientName,
            @RequestPart("diseaseDescription") String diseaseDescription,
            @RequestPart("patientDetails") String patientDetails,
            @RequestPart(value = "transactionId", required = false) String transactionId,
            @RequestPart("paymentStatus") String paymentStatus,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        
        String attachmentUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = "uploads/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                attachmentUrl = "/uploads/" + fileName;
            } catch (IOException e) {
                return ResponseEntity.status(500).body("Could not upload file: " + e.getMessage());
            }
        }

        Appointment emergency = appointmentService.handleEmergency(userDetails.getId(), patientName, diseaseDescription, patientDetails, transactionId, attachmentUrl, paymentStatus);

        return ResponseEntity.ok(emergency);
    }

    @GetMapping("/emergency/available-slot")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getAvailableSlot() {
        java.time.LocalTime slot = appointmentService.getNextAvailableEmergencySlot();
        if (slot == null) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", "No emergency slots available for today."));
        }
        return ResponseEntity.ok(java.util.Collections.singletonMap("slot", slot.toString()));
    }

    @PutMapping("/{id}/confirm-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
        Appointment appointment = appointmentService.confirmEmergencyPayment(id);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{id}/cancel-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelPayment(@PathVariable Long id) {
        Appointment appointment = appointmentService.cancelEmergencyPayment(id);
        return ResponseEntity.ok(appointment);
    }

    // DOCTOR or PATIENT: View Appointments
    @GetMapping("/my")
    public ResponseEntity<?> getMyAppointments() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        boolean isDoctor = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        List<Appointment> apps;
        if (isDoctor) {
            // Find the doctor profile associated with this user ID
            com.project.entity.Doctor doctor = appointmentService.getDoctorProfileByUserId(userDetails.getId());
            if (doctor != null) {
                apps = appointmentService.getDoctorAppointments(doctor.getId());
            } else {
                apps = java.util.Collections.emptyList();
            }
        } else {
            apps = appointmentService.getPatientAppointments(userDetails.getId());
        }
        return ResponseEntity.ok(apps);
    }

    // DOCTOR or ADMIN: Update Status
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Appointment appointment = appointmentService.updateAppointmentStatus(id, status);
        return ResponseEntity.ok(appointment);
    }

    // ADMIN: View All Appointments
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }
}
