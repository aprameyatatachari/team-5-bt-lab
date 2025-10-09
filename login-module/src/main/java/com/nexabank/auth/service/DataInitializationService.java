package com.nexabank.auth.service;

import com.nexabank.auth.entity.User;
import com.nexabank.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createDefaultAdminIfNotExists();
    }

    private void createDefaultAdminIfNotExists() {
        String adminEmail = "admin@nexabank.com";
        
        if (!userRepository.existsByEmail(adminEmail)) {
            System.out.println("Creating default admin account...");
            
            // Create admin user
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setUserType(User.UserType.ADMIN);
            admin.setStatus(User.UserStatus.ACTIVE);
            admin.setPasswordChangedAt(LocalDateTime.now());
            admin.setMustChangePassword(false);
            
            // Set admin roles
            Set<User.Role> adminRoles = new HashSet<>();
            adminRoles.add(User.Role.ADMIN_FULL_ACCESS);
            adminRoles.add(User.Role.ADMIN_USER_MANAGEMENT);
            adminRoles.add(User.Role.ADMIN_SYSTEM_CONFIG);
            adminRoles.add(User.Role.ADMIN_REPORTS);
            adminRoles.add(User.Role.ADMIN_VIEW);
            admin.setRoles(adminRoles);
            
            userRepository.save(admin);
            
            System.out.println("Default admin account created successfully!");
            System.out.println("Email: " + adminEmail);
            System.out.println("Password: admin123");
            System.out.println("Please change the password after first login for security.");
            
        } else {
            System.out.println("Admin account already exists. Skipping creation.");
        }
    }
}