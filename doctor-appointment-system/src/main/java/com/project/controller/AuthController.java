package com.project.controller;

import com.project.dto.ForgotPasswordRequest;
import com.project.dto.ResetPasswordRequest;
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

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(new MessageResponse("Pong"));
    }

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

        // Check if email already exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already registered!"));
        }

        // Create new user account
        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setMobileNumber(signUpRequest.getMobileNumber());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        // Assign role — default to PATIENT if invalid
        Role role = Role.PATIENT;
        if (signUpRequest.getRole() != null) {
            try {
                role = Role.valueOf(signUpRequest.getRole());
            } catch (IllegalArgumentException e) {
                // default to PATIENT
            }
        }

        user.setRole(role);
        user = userRepository.save(user);

        // If Doctor, create doctor profile
        if (role == Role.DOCTOR) {
            if (signUpRequest.getLicenseNumber() == null || signUpRequest.getLicenseNumber().trim().length() < 5) {
                userRepository.delete(user);
                return ResponseEntity.badRequest().body(
                        new MessageResponse("Error: Doctor registration requires a valid License Number"));
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

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String identifier = request.getIdentifier();
        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByMobileNumber(identifier);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User with this Email/Mobile not found!"));
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(identifier, otp);

        // Simulate sending — print to console for demo
        System.out.println("==========================================");
        System.out.println("FORGOT PASSWORD OTP FOR " + identifier + ": " + otp);
        System.out.println("==========================================");

        return ResponseEntity.ok(new MessageResponse("OTP sent successfully to your registered Email/Mobile (check console logs)"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String identifier = request.getIdentifier();
        String storedOtp = otpStore.get(identifier);

        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired OTP!"));
        }

        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByMobileNumber(identifier);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Something went wrong, user not found."));
        }

        User user = userOpt.get();
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Clear OTP after successful use
        otpStore.remove(identifier);

        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully. You can now login."));
    }
}
