package com.homy.backend.repository;

import com.homy.backend.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    Optional<Technician> findByEmail(String email);
    Optional<Technician> findTopByOrderByIdDesc();
    Optional<Technician> findByTechnicianId(String technicianId);
    Optional<Technician> findByPhone(String phone);

    // Find active technicians for a particular service category
    java.util.List<Technician> findByServiceCategoryAndIsActiveTrue(String serviceCategory);
}
