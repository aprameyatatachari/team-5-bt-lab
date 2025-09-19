package com.nexabank.customer.service;

import com.nexabank.customer.dto.CreateUserRequest;
import com.nexabank.customer.dto.UserDto;
import com.nexabank.customer.entity.User;
import com.nexabank.customer.entity.UserSession;
import com.nexabank.customer.repository.UserRepository;
import com.nexabank.customer.repository.UserSessionRepository;
import com.nexabank.customer.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerAuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CustomerUserIntegrationService integrationService;

    @Value("${app.jwt.expiration:86400000}")
    private Long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private Long jwtRefreshExpirationMs;

    /**
     * Register a new user directly in customer module
     */
    public UserDto registerUser(CreateUserRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Create user entity with ALL details from the request
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // Hash the password
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setUserType(request.getUserType() != null ? request.getUserType() : "CUSTOMER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // Map additional details from CreateUserRequest
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
            try {
                user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
            } catch (Exception e) {
                log.warn("Invalid date of birth format: {}", request.getDateOfBirth());
            }
        }
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        user.setPostalCode(request.getPostalCode());
        user.setAadharNumber(request.getAadharNumber());
        user.setPanNumber(request.getPanNumber());

        // Save user to customer database
        User savedUser = userRepository.save(user);

        // Create comprehensive customer details
        try {
            UserDto userDto = convertToUserDto(savedUser);
            integrationService.createCustomerFromUser(userDto);
            log.info("Customer details created for user: {}", savedUser.getUserId());
        } catch (Exception e) {
            log.error("Failed to create customer details: {}", e.getMessage());
            // Don't fail the registration, customer details can be created later
        }

        log.info("User registered successfully: {}", savedUser.getUserId());
        return convertToUserDto(savedUser);
    }

    /**
     * Login user with email and password
     */
    public Map<String, Object> loginUser(String email, String password) {
        log.info("Attempting login for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();
        
        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // Increment failed login attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw new RuntimeException("Invalid email or password");
        }
        
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        // Reset failed login attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());

        // Generate JWT tokens
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), email, "CUSTOMER");
        String refreshToken = jwtUtils.generateRefreshToken(user.getUserId());

        // Create or update user session
        createOrUpdateUserSession(user.getUserId(), accessToken, refreshToken);

        // Update last login
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("user", convertToUserDto(user));
        response.put("expiresIn", jwtExpirationMs);

        log.info("Login successful for user: {}", user.getUserId());
        return response;
    }

    /**
     * Validate JWT token with comprehensive checks
     */
    public Map<String, Object> validateToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token format");
        }

        String jwt = token.substring(7);
        
        // First validate JWT signature and expiration
        if (!jwtUtils.validateJwtToken(jwt)) {
            throw new RuntimeException("Invalid or expired token");
        }

        String userId;
        try {
            userId = jwtUtils.getUserIdFromJwtToken(jwt);
        } catch (Exception e) {
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }

        // Check if user exists and is active
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        // Check if session exists and is valid
        Optional<UserSession> sessionOpt = userSessionRepository.findByUserIdAndAccessToken(userId, jwt);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Session not found or expired");
        }

        UserSession session = sessionOpt.get();
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Clean up expired session
            userSessionRepository.deleteByUserIdAndAccessToken(userId, jwt);
            throw new RuntimeException("Session expired");
        }

        // Update session last accessed time
        session.setUpdatedAt(LocalDateTime.now());
        userSessionRepository.save(session);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("user", convertToUserDto(user));
        response.put("expiresAt", session.getExpiresAt());
        response.put("sessionId", session.getSessionId());

        return response;
    }

    /**
     * Logout user
     */
    public void logoutUser(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new RuntimeException("Invalid token format");
            }

            String jwt = token.substring(7);
            
            // Validate token first
            if (!jwtUtils.validateJwtToken(jwt)) {
                log.warn("Attempting to logout with invalid token");
                // Still proceed with logout to clean up any potential sessions
            }

            String userId = jwtUtils.getUserIdFromJwtToken(jwt);
            
            // Delete all sessions for this user (more thorough cleanup)
            int deletedSessions = userSessionRepository.deleteByUserId(userId);
            log.info("User logged out successfully: {} (deleted {} sessions)", userId, deletedSessions);
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            // Still allow logout to complete to ensure user gets logged out on frontend
        }
    }

    /**
     * Logout user from all devices
     */
    public void logoutUserFromAllDevices(String userId) {
        try {
            int deletedSessions = userSessionRepository.deleteByUserId(userId);
            log.info("User logged out from all devices: {} (deleted {} sessions)", userId, deletedSessions);
        } catch (Exception e) {
            log.error("Error during logout from all devices: {}", e.getMessage());
            throw new RuntimeException("Failed to logout from all devices: " + e.getMessage());
        }
    }

    /**
     * Logout user from all devices using token
     */
    public void logoutUserFromAllDevicesWithToken(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new RuntimeException("Invalid token format");
            }

            String jwt = token.substring(7);
            String userId = jwtUtils.getUserIdFromJwtToken(jwt);
            
            logoutUserFromAllDevices(userId);
        } catch (Exception e) {
            log.error("Error during logout from all devices with token: {}", e.getMessage());
            throw new RuntimeException("Failed to logout from all devices: " + e.getMessage());
        }
    }

    /**
     * Refresh access token
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String userId = jwtUtils.getUserIdFromJwtToken(refreshToken);
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // Check if refresh token exists in session
        Optional<UserSession> sessionOpt = userSessionRepository.findByUserIdAndRefreshToken(userId, refreshToken);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        UserSession session = sessionOpt.get();
        if (session.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        // Generate new tokens
        String newAccessToken = jwtUtils.generateAccessToken(userId, user.getEmail(), user.getUserType());
        String newRefreshToken = jwtUtils.generateRefreshToken(userId);

        // Update session
        session.setAccessToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpirationMs / 1000));
        session.setRefreshExpiresAt(LocalDateTime.now().plusSeconds(jwtRefreshExpirationMs / 1000));
        session.setUpdatedAt(LocalDateTime.now());
        userSessionRepository.save(session);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("refreshToken", newRefreshToken);
        response.put("user", convertToUserDto(user));
        response.put("expiresIn", jwtExpirationMs);

        return response;
    }

    private void createOrUpdateUserSession(String userId, String accessToken, String refreshToken) {
        try {
            // Delete existing sessions for user (single session per user policy)
            int deletedSessions = userSessionRepository.deleteByUserId(userId);
            if (deletedSessions > 0) {
                log.info("Deleted {} existing sessions for user: {}", deletedSessions, userId);
            }

            // Create new session
            UserSession session = new UserSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setUserId(userId);
            session.setAccessToken(accessToken);
            session.setRefreshToken(refreshToken);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpirationMs / 1000));
            session.setRefreshExpiresAt(LocalDateTime.now().plusSeconds(jwtRefreshExpirationMs / 1000));

            userSessionRepository.save(session);
            log.info("Created new session for user: {} (session: {})", userId, session.getSessionId());
        } catch (Exception e) {
            log.error("Error creating user session: {}", e.getMessage());
            throw new RuntimeException("Failed to create session: " + e.getMessage());
        }
    }

    private UserDto convertToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setUserType(user.getUserType());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setCountry(user.getCountry());
        dto.setPostalCode(user.getPostalCode());
        // For security, mask sensitive data
        if (user.getAadharNumber() != null) {
            dto.setMaskedAadhar(maskAadhar(user.getAadharNumber()));
        }
        if (user.getPanNumber() != null) {
            dto.setMaskedPan(maskPan(user.getPanNumber()));
        }
        return dto;
    }
    
    private String maskAadhar(String aadhar) {
        if (aadhar == null || aadhar.length() < 4) return aadhar;
        return "****-****-" + aadhar.substring(aadhar.length() - 4);
    }
    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return pan;
        return "******" + pan.substring(pan.length() - 4);
    }
}
