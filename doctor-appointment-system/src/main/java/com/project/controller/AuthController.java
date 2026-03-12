package com.project.controller;

import com.project.dto.JwtResponse;
import com.project.dto.LoginRequest;
import com.project.dto.MessageResponse;
import com.project.dto.SignupRequest;
import com.project.entity.Doctor;
import com.project.entity.Role;
import com.project.entity.User;
import com.project.repository.DoctorRepository;
import com.project.repository.UserRepository;
import com.project.security.JwtUtils;
import com.project.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getIdentifier(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        if ("DOCTOR".equals(role)) {
            Doctor doctorProfile = doctorRepository.findAll().stream()
                    .filter(d -> d.getUser().getId().equals(userDetails.getId()))
                    .findFirst()
                    .orElse(null);

            if (doctorProfile != null && "PENDING".equals(doctorProfile.getStatus())) {
                return ResponseEntity.status(403).body(new MessageResponse("Account pending admin approval"));
            }
        }

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getName(),
                userDetails.getUsername(),
                role));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        
        // Manual validation: Must provide EITHER email OR mobile number
        boolean hasEmail = signUpRequest.getEmail() != null && !signUpRequest.getEmail().trim().isEmpty();
        boolean hasMobile = signUpRequest.getMobileNumber() != null && !signUpRequest.getMobileNumber().trim().isEmpty();
        
        if (!hasEmail && !hasMobile) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: You must provide either an Email or a Mobile Number!"));
        }

        if (hasEmail && userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        if (hasMobile && userRepository.existsByMobileNumber(signUpRequest.getMobileNumber())) {
             return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Mobile number is already in use!"));
        }

        // Generate a static 6-digit PIN for the user
        String generatedPin = String.format("%06d", new java.util.Random().nextInt(999999));

        // Create new user's account
        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(hasEmail ? signUpRequest.getEmail() : null); // Keep it strictly null if not provided
        user.setMobileNumber(hasMobile ? signUpRequest.getMobileNumber() : null);
        user.setPassword(encoder.encode(generatedPin)); // Save the generated PIN as the password

        // Try to assign the requested role, default to PATIENT if none specified or invalid
        Role role = Role.PATIENT;
        if (signUpRequest.getRole() != null) {
            System.out.println("Processing role request: " + signUpRequest.getRole());
            try {
                // Ensure the exact enum name matched (e.g. PATIENT, DOCTOR)
                role = Role.valueOf(signUpRequest.getRole());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid role specified, defaulting to PATIENT: " + signUpRequest.getRole());
            }
        }
        
        user.setRole(role);
        user = userRepository.save(user);

        // If Doctor, create doctor profile
        if (role == Role.DOCTOR) {
            if (signUpRequest.getLicenseNumber() == null || signUpRequest.getLicenseNumber().trim().length() < 5) {
                userRepository.delete(user); // rollback user creation to prevent orphaned accounts
                return ResponseEntity.badRequest().body(
                        new MessageResponse("Error: Doctor registration requires a valid verified License Number"));
            }
            Doctor doctor = new Doctor();
            doctor.setUser(user);
            doctor.setSpecialization(
                    signUpRequest.getSpecialization() != null && !signUpRequest.getSpecialization().isEmpty()
                            ? signUpRequest.getSpecialization()
                            : "General");
            doctor.setSchedule("Not Set");
            doctor.setLicenseNumber(signUpRequest.getLicenseNumber().trim());
            doctorRepository.save(doctor);
        }

        return ResponseEntity.ok(new MessageResponse("User registered successfully!\nYour 6-Digit PIN is: " + generatedPin));
    }
}
