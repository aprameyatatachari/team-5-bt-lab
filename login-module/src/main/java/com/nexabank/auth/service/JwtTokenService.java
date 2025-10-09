package com.nexabank.auth.service;

import com.nexabank.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    @Value("${jwt.secret:mySecretKey}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    // Simplified JWT implementation with role support
    public String generateToken(String email, String userId, String userType, Set<User.Role> roles) {
        // Convert roles to string representation
        String rolesString = roles != null ? 
            roles.stream().map(Enum::name).collect(Collectors.joining(",")) : "";
            
        // Enhanced token format with roles
        return "JWT_" + email + "_" + userId + "_" + userType + "_" + rolesString + "_" + System.currentTimeMillis();
    }

    public String generateTokenForUser(User user) {
        return generateToken(user.getEmail(), user.getUserId(), user.getUserType().name(), user.getRoles());
    }

    public boolean validateToken(String token) {
        // Simple validation - check if token starts with JWT_ and has valid parts
        if (token == null || !token.startsWith("JWT_")) {
            return false;
        }
        
        String[] parts = token.split("_");
        return parts.length >= 5; // email, userId, userType, roles, timestamp
    }

    public String extractUsername(String token) {
        if (token != null && token.startsWith("JWT_")) {
            String[] parts = token.split("_");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    public String extractUserId(String token) {
        if (token != null && token.startsWith("JWT_")) {
            String[] parts = token.split("_");
            if (parts.length > 2) {
                return parts[2];
            }
        }
        return null;
    }

    public String extractUserType(String token) {
        if (token != null && token.startsWith("JWT_")) {
            String[] parts = token.split("_");
            if (parts.length > 3) {
                return parts[3];
            }
        }
        return null;
    }

    public Set<User.Role> extractRoles(String token) {
        if (token != null && token.startsWith("JWT_")) {
            String[] parts = token.split("_");
            if (parts.length > 4 && !parts[4].isEmpty()) {
                String[] roleNames = parts[4].split(",");
                return java.util.Arrays.stream(roleNames)
                    .map(roleName -> {
                        try {
                            return Enum.valueOf(User.Role.class, roleName.trim());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(role -> role != null)
                    .collect(Collectors.toSet());
            }
        }
        return java.util.Collections.emptySet();
    }

    public boolean hasRole(String token, User.Role role) {
        Set<User.Role> roles = extractRoles(token);
        return roles.contains(role);
    }

    public boolean hasAnyRole(String token, User.Role... requiredRoles) {
        Set<User.Role> tokenRoles = extractRoles(token);
        for (User.Role role : requiredRoles) {
            if (tokenRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin(String token) {
        return hasAnyRole(token, User.Role.ADMIN_FULL_ACCESS, User.Role.ADMIN_USER_MANAGEMENT);
    }

    public boolean isCustomer(String token) {
        return hasAnyRole(token, User.Role.CUSTOMER_VIEW, User.Role.CUSTOMER_TRANSACTION);
    }

    // Additional methods needed by controllers
    public String extractEmail(String token) {
        return extractUsername(token);
    }

    public String generateAccessToken(String email) {
        // Default access token with minimal customer roles
        Set<User.Role> defaultRoles = Set.of(User.Role.CUSTOMER_VIEW);
        return generateToken(email, "user_" + System.currentTimeMillis(), "CUSTOMER", defaultRoles);
    }

    public String generateAccessTokenForUser(User user) {
        return generateTokenForUser(user);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, "refresh_" + System.currentTimeMillis(), "REFRESH", Set.of());
    }

    public String generateRefreshTokenForUser(User user) {
        // Refresh tokens don't need full role information
        return generateToken(user.getEmail(), user.getUserId(), "REFRESH", Set.of());
    }

    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    public void blacklistToken(String token) {
        // Simple blacklist implementation - in production use Redis
        // For now, just log that token is blacklisted
        System.out.println("Token blacklisted: " + token);
    }

    // Legacy methods for compatibility
    public String generateToken(String email, String userId, String userType) {
        Set<User.Role> defaultRoles = Set.of(User.Role.CUSTOMER_VIEW);
        return generateToken(email, userId, userType, defaultRoles);
    }
}
