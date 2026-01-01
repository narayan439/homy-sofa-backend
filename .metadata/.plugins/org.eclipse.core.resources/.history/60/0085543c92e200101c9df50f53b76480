package com.homy.backend.repository;

import com.homy.backend.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminUser, String> {
    Optional<AdminUser> findByEmail(String email);
}
