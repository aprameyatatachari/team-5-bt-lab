package com.nexabank.auth.service;

import com.nexabank.auth.dto.AuthResponse;
import com.nexabank.auth.dto.LoginRequest;
import com.nexabank.auth.dto.RegisterRequest;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.entity.UserSession;
import com.nexabank.auth.exception.BadRequestException;
import com.nexabank.auth.exception.UnauthorizedException;
import com.nexabank.auth.repository.UserRepository;
import com.nexabank.auth.repository.UserSessionRepository;
import com.nexabank.auth.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserSessionRepository userSessionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Value("${app.jwt.expiration}")
    private int jwtExpirationMs;
    
    @Transactional
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email is already taken!");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setDateOfBirth(registerRequest.getDateOfBirth());
        user.setAddress(registerRequest.getAddress());
        user.setCity(registerRequest.getCity());
        user.setState(registerRequest.getState());
        user.setCountry(registerRequest.getCountry());
        user.setPostalCode(registerRequest.getPostalCode());
        user.setAadharNumber(registerRequest.getAadharNumber());
        user.setPanNumber(registerRequest.getPanNumber());
        user.setUserType(registerRequest.getUserType());
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtUtils.generateTokenFromUsername(savedUser.getEmail());
        String refreshToken = jwtUtils.generateRefreshToken(savedUser.getEmail());
        
        // Create user session
        createUserSession(savedUser, accessToken, refreshToken, null);
        
        logger.info("User registered successfully with email: {}", savedUser.getEmail());
        
        return createAuthResponse(accessToken, refreshToken, savedUser);
    }
    
    @Transactional
    public AuthResponse authenticateUser(LoginRequest loginRequest, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Generate JWT token
            String jwt = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(loginRequest.getEmail());
            
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));
            
            // Update last login and reset failed attempts
            user.setLastLogin(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            
            // Invalidate old sessions if not remember me
            if (!loginRequest.isRememberMe()) {
                invalidateUserSessions(user);
            }
            
            // Create new session
            createUserSession(user, jwt, refreshToken, request);
            
            logger.info("User authenticated successfully: {}", user.getEmail());
            
            return createAuthResponse(jwt, refreshToken, user);
            
        } catch (BadCredentialsException e) {
            // Handle failed login attempt
            User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
            if (user != null) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setStatus(User.UserStatus.LOCKED);
                    logger.warn("User account locked due to multiple failed attempts: {}", user.getEmail());
                }
                userRepository.save(user);
            }
            throw new UnauthorizedException("Invalid email or password");
        }
    }
    
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateJwtToken(refreshToken) || 
            !"refresh".equals(jwtUtils.getTokenType(refreshToken))) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        
        String email = jwtUtils.getUserNameFromJwtToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }
        
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        
        if (!session.getIsActive()) {
            throw new UnauthorizedException("Session is not active");
        }
        
        // Generate new tokens
        String newAccessToken = jwtUtils.generateTokenFromUsername(email);
        String newRefreshToken = jwtUtils.generateRefreshToken(email);
        
        // Update session
        session.setAccessToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpirationMs / 1000));
        userSessionRepository.save(session);
        
        return createAuthResponse(newAccessToken, newRefreshToken, user);
    }
    
    @Transactional
    public void logout(String accessToken) {
        userSessionRepository.findByAccessToken(accessToken)
                .ifPresent(session -> {
                    session.setIsActive(false);
                    userSessionRepository.save(session);
                });
    }
    
    @Transactional
    public void logoutAllDevices(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        invalidateUserSessions(user);
    }
    
    private void createUserSession(User user, String accessToken, String refreshToken, HttpServletRequest request) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpirationMs / 1000));
        session.setCreatedAt(LocalDateTime.now());
        session.setIsActive(true);
        
        if (request != null) {
            session.setIpAddress(getClientIpAddress(request));
            session.setUserAgent(request.getHeader("User-Agent"));
        }
        
        userSessionRepository.save(session);
    }
    
    private void invalidateUserSessions(User user) {
        List<UserSession> activeSessions = userSessionRepository.findByUserUserIdAndIsActiveTrue(user.getUserId());
        activeSessions.forEach(session -> session.setIsActive(false));
        userSessionRepository.saveAll(activeSessions);
    }
    
    private AuthResponse createAuthResponse(String accessToken, String refreshToken, User user) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn((long) jwtExpirationMs / 1000);
        response.setUser(AuthResponse.UserInfo.fromUser(user));
        return response;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        }
        return xForwardedForHeader.split(",")[0];
    }
    
    @Transactional
    public void cleanupExpiredSessions() {
        userSessionRepository.deleteExpiredSessions(LocalDateTime.now());
        logger.info("Cleaned up expired sessions");
    }
}
