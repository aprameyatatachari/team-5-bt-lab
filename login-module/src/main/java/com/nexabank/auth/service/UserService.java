package com.nexabank.auth.service;

import com.nexabank.auth.entity.User;
import com.nexabank.auth.exception.AuthenticationException;
import com.nexabank.auth.exception.UserAlreadyExistsException;
import com.nexabank.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerRegistrationService customerRegistrationService;

    public User authenticate(String email, String password) throws AuthenticationException {
        Optional<User> userOptional = userRepository.findActiveUserByEmail(email);

        if (userOptional.isEmpty()) {
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userOptional.get();

        // Check if user account is locked
        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new AuthenticationException("Account is locked. Please contact support.");
        }

        // Check if user account is inactive
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new AuthenticationException("Account is inactive. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // Increment failed login attempts
            user.setFailedLoginAttempts((user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1);
            
            // Lock account temporarily after 3 failed attempts
            if (user.getFailedLoginAttempts() >= 3) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
                user.setStatus(User.UserStatus.LOCKED);
            }
            
            userRepository.save(user);
            throw new AuthenticationException("Invalid email or password");
        }

        // Reset failed login attempts on successful authentication
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return user;
    }

    @Transactional
    public User registerUser(String email, String password, String firstName, String lastName, String phoneNumber, String userType) throws UserAlreadyExistsException {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        // Create new user with authentication data only
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        // Set user type and default roles
        User.UserType type;
        try {
            type = userType != null ? Enum.valueOf(User.UserType.class, userType.toUpperCase()) : User.UserType.CUSTOMER;
        } catch (IllegalArgumentException e) {
            type = User.UserType.CUSTOMER; // Default to customer if invalid userType provided
        }
        user.setUserType(type);
        user.setRoles(getDefaultRolesForUserType(type));
        
        user.setStatus(User.UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);

        // Save user in auth database
        User savedUser = userRepository.save(user);
        
        // Register user in appropriate module based on user type
        if (type == User.UserType.CUSTOMER) {
            boolean customerRegistered = customerRegistrationService.registerCustomerInCustomerModule(
                savedUser, firstName, lastName, phoneNumber);
            if (!customerRegistered) {
                // Log warning but don't fail registration
                System.err.println("Warning: Failed to register customer in customer module for user: " + savedUser.getEmail());
            }
        } else if (type == User.UserType.ADMIN || type == User.UserType.EMPLOYEE) {
            boolean profileCreated = customerRegistrationService.createAdminProfile(
                savedUser, firstName, lastName, phoneNumber);
            if (!profileCreated) {
                // Log warning but don't fail registration
                System.err.println("Warning: Failed to create admin/employee profile for user: " + savedUser.getEmail());
            }
        }

        return savedUser;
    }

    private Set<User.Role> getDefaultRolesForUserType(User.UserType userType) {
        Set<User.Role> roles = new HashSet<>();
        
        switch (userType) {
            case CUSTOMER:
                roles.add(User.Role.CUSTOMER_VIEW);
                roles.add(User.Role.CUSTOMER_TRANSACTION);
                break;
            case EMPLOYEE:
                roles.add(User.Role.EMPLOYEE_VIEW);
                roles.add(User.Role.EMPLOYEE_CUSTOMER_MANAGEMENT);
                break;
            case ADMIN:
                roles.add(User.Role.ADMIN_VIEW);
                roles.add(User.Role.ADMIN_USER_MANAGEMENT);
                roles.add(User.Role.ADMIN_SYSTEM_CONFIG);
                roles.add(User.Role.ADMIN_REPORTS);
                roles.add(User.Role.ADMIN_FULL_ACCESS);
                break;
            case SYSTEM:
                roles.add(User.Role.SYSTEM_API_ACCESS);
                break;
            default:
                roles.add(User.Role.CUSTOMER_VIEW);
        }
        
        return roles;
    }

    @Transactional
    public void addRoleToUser(String userId, User.Role role) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getRoles() == null) {
                user.setRoles(new HashSet<>());
            }
            user.getRoles().add(role);
            userRepository.save(user);
        }
    }

    @Transactional
    public void removeRoleFromUser(String userId, User.Role role) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getRoles() != null) {
                user.getRoles().remove(role);
                userRepository.save(user);
            }
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    @Transactional
    public void updateLastLogin(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    public boolean isUserActive(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.isPresent() && userOptional.get().getStatus() == User.UserStatus.ACTIVE;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean updateUserStatus(String userId, User.UserStatus status) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(status);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public Map<String, Object> getUserStatistics() {
        List<User> allUsers = userRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", allUsers.size());
        
        long activeUsers = allUsers.stream()
            .filter(user -> user.getStatus() == User.UserStatus.ACTIVE)
            .count();
        stats.put("activeUsers", activeUsers);
        
        long inactiveUsers = allUsers.stream()
            .filter(user -> user.getStatus() == User.UserStatus.INACTIVE)
            .count();
        stats.put("inactiveUsers", inactiveUsers);
        
        long suspendedUsers = allUsers.stream()
            .filter(user -> user.getStatus() == User.UserStatus.SUSPENDED)
            .count();
        stats.put("suspendedUsers", suspendedUsers);
        
        long customerUsers = allUsers.stream()
            .filter(user -> user.getUserType() == User.UserType.CUSTOMER)
            .count();
        stats.put("customerUsers", customerUsers);
        
        long adminUsers = allUsers.stream()
            .filter(user -> user.getUserType() == User.UserType.ADMIN || user.getUserType() == User.UserType.EMPLOYEE)
            .count();
        stats.put("adminUsers", adminUsers);
        
        // Recent registrations (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentRegistrations = allUsers.stream()
            .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        stats.put("recentRegistrations", recentRegistrations);
        
        return stats;
    }
}
