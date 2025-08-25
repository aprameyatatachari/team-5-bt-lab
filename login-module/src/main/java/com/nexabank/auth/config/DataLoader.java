package com.nexabank.auth.config;

import com.nexabank.auth.entity.User;
import com.nexabank.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        loadInitialData();
    }

    private void loadInitialData() {
        // Create default admin user if not exists
        if (!userRepository.existsByEmail("admin@nexabank.com")) {
            User admin = new User();
            admin.setEmail("admin@nexabank.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setPhoneNumber("+1-555-0000");
            admin.setUserType(User.UserType.ADMIN);
            admin.setStatus(User.UserStatus.ACTIVE);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);
            System.out.println("Created default admin user: admin@nexabank.com / Admin@123");
        }

        // Create default customer user if not exists
        if (!userRepository.existsByEmail("customer@nexabank.com")) {
            User customer = new User();
            customer.setEmail("customer@nexabank.com");
            customer.setPasswordHash(passwordEncoder.encode("Customer@123"));
            customer.setFirstName("Demo");
            customer.setLastName("Customer");
            customer.setPhoneNumber("+1-555-0001");
            customer.setUserType(User.UserType.CUSTOMER);
            customer.setStatus(User.UserStatus.ACTIVE);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setUpdatedAt(LocalDateTime.now());
            userRepository.save(customer);
            System.out.println("Created default customer user: customer@nexabank.com / Customer@123");
        }

        // Create default employee user if not exists
        if (!userRepository.existsByEmail("employee@nexabank.com")) {
            User employee = new User();
            employee.setEmail("employee@nexabank.com");
            employee.setPasswordHash(passwordEncoder.encode("Employee@123"));
            employee.setFirstName("Demo");
            employee.setLastName("Employee");
            employee.setPhoneNumber("+1-555-0002");
            employee.setUserType(User.UserType.EMPLOYEE);
            employee.setStatus(User.UserStatus.ACTIVE);
            employee.setCreatedAt(LocalDateTime.now());
            employee.setUpdatedAt(LocalDateTime.now());
            userRepository.save(employee);
            System.out.println("Created default employee user: employee@nexabank.com / Employee@123");
        }
    }
}
