package com.project.repository;

import com.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    Boolean existsByMobileNumber(String mobileNumber);

    java.util.List<User> findByRole(com.project.entity.Role role);
}
