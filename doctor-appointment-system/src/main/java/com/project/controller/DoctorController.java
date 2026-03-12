package com.project.controller;

import com.project.entity.Doctor;
import com.project.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping("/all")
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorRepository.findAll());
    }

    @GetMapping("/pending")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Doctor>> getPendingDoctors() {
        // We find all doctors and filter in memory for simplicity, though a DB query is
        // better for prod
        return ResponseEntity.ok(doctorRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .toList());
    }

    @PutMapping("/approve/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveDoctor(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(() -> new RuntimeException("Doctor not found"));
        doctor.setStatus("APPROVED");
        doctorRepository.save(doctor);
        return ResponseEntity.ok(new com.project.dto.MessageResponse("Doctor approved successfully"));
    }
}
