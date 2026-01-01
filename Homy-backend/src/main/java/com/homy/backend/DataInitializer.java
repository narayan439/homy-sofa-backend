package com.homy.backend;

import com.homy.backend.model.AdminUser;
import com.homy.backend.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        final String adminEmail = "admin@homysofa.com";
        if (adminRepository.findByEmail(adminEmail).isEmpty()) {
            AdminUser admin = new AdminUser();
            admin.setEmail(adminEmail);
            admin.setName("Administrator");
            admin.setPassword(passwordEncoder.encode("123456"));
            adminRepository.save(admin);
            System.out.println("[DataInitializer] Created default admin: " + adminEmail + " / 123456");
        } else {
            System.out.println("[DataInitializer] admin already exists: " + adminEmail);
        }
    }
}
