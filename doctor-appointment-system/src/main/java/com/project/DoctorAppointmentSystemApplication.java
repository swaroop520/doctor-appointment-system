package com.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.project.entity.User;
import com.project.entity.Role;
import com.project.entity.Doctor;
import com.project.repository.UserRepository;
import com.project.repository.DoctorRepository;

@SpringBootApplication
public class DoctorAppointmentSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoctorAppointmentSystemApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository, DoctorRepository doctorRepository,
			PasswordEncoder encoder) {
		return args -> {
			if (!userRepository.existsByEmail("admin@test.com")) {
				User admin = new User();
				admin.setName("System Admin");
				admin.setEmail("admin@test.com");
				admin.setPassword(encoder.encode("admin123"));
				admin.setRole(Role.ADMIN);
				userRepository.save(admin);
			}

			if (!userRepository.existsByEmail("doctor@test.com")) {
				User docUser = new User();
				docUser.setName("Dr. Sarah Smith");
				docUser.setEmail("doctor@test.com");
				docUser.setPassword(encoder.encode("doctor123"));
				docUser.setRole(Role.DOCTOR);
				userRepository.save(docUser);

				Doctor doctor = new Doctor();
				doctor.setUser(docUser);
				doctor.setSpecialization("Cardiologist");
				doctor.setSchedule("Mon-Fri 9AM-5PM");
				doctor.setLicenseNumber("LIC-10001");
				doctorRepository.save(doctor);
			}

			if (!userRepository.existsByEmail("derm@test.com")) {
				User docUser2 = new User();
				docUser2.setName("Dr. Emily Chen");
				docUser2.setEmail("derm@test.com");
				docUser2.setPassword(encoder.encode("doctor123"));
				docUser2.setRole(Role.DOCTOR);
				userRepository.save(docUser2);

				Doctor doctor2 = new Doctor();
				doctor2.setUser(docUser2);
				doctor2.setSpecialization("Dermatologist");
				doctor2.setSchedule("Mon-Wed 8AM-2PM");
				doctor2.setLicenseNumber("LIC-20002");
				doctorRepository.save(doctor2);
			}

			if (!userRepository.existsByEmail("ortho@test.com")) {
				User docUser3 = new User();
				docUser3.setName("Dr. Michael Brown");
				docUser3.setEmail("ortho@test.com");
				docUser3.setPassword(encoder.encode("doctor123"));
				docUser3.setRole(Role.DOCTOR);
				userRepository.save(docUser3);

				Doctor doctor3 = new Doctor();
				doctor3.setUser(docUser3);
				doctor3.setSpecialization("Orthopedic Surgeon");
				doctor3.setSchedule("Tue-Thu 10AM-6PM");
				doctor3.setLicenseNumber("LIC-30003");
				doctorRepository.save(doctor3);
			}

			if (!userRepository.existsByEmail("peds@test.com")) {
				User docUser4 = new User();
				docUser4.setName("Dr. Jessica Taylor");
				docUser4.setEmail("peds@test.com");
				docUser4.setPassword(encoder.encode("doctor123"));
				docUser4.setRole(Role.DOCTOR);
				userRepository.save(docUser4);

				Doctor doctor4 = new Doctor();
				doctor4.setUser(docUser4);
				doctor4.setSpecialization("Pediatrician");
				doctor4.setSchedule("Mon-Fri 8AM-4PM");
				doctor4.setLicenseNumber("LIC-40004");
				doctorRepository.save(doctor4);
			}
		};
	}
}
