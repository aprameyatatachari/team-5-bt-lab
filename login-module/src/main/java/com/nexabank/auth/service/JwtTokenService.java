package com.nexabank.auth.service;

import com.nexabank.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    @Value("${jwt.secret:mySecretKeyThatShouldBeAtLeast32CharactersLongForSecurity}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    @Autowired
    private RedisSessionService redisSessionService;

    private SecretKey getSigningKey() {
        // Ensure secret is at least 32 characters for security
        String secret = jwtSecret;
        if (secret.length() < 32) {
            secret = secret + "padding".repeat((32 - secret.length()) / 7 + 1);
            secret = secret.substring(0, 32);
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String userId, String userType, Set<User.Role> roles) {
        String jti = UUID.randomUUID().toString(); // Unique JWT ID
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration);
        
        // Convert roles to string representation
        String rolesString = roles != null ? 
            roles.stream().map(Enum::name).collect(Collectors.joining(",")) : "";
        
        return Jwts.builder()
                .subject(email)
                .id(jti) // JWT ID for denylist tracking
                .claim("userId", userId)
                .claim("userType", userType)
                .claim("roles", rolesString)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenForUser(User user) {
        return generateToken(user.getEmail(), user.getUserId(), user.getUserType().name(), user.getRoles());
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
            // Check if token is on denylist (blacklisted)
            String jti = claims.getId();
            if (jti != null && redisSessionService.isTokenDenylisted(jti)) {
                System.out.println("Token rejected: JWT ID " + jti + " is on denylist");
                return false;
            }
            
            return true; // Token is valid and not denylisted
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUserType(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userType", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractJwtId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (Exception e) {
            return null;
        }
    }

    public Date extractExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public Set<User.Role> extractRoles(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
            String rolesString = claims.get("roles", String.class);
            if (rolesString != null && !rolesString.isEmpty()) {
                String[] roleNames = rolesString.split(",");
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
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return java.util.Collections.emptySet();
    }

    /**
     * Add JWT to denylist for immediate logout
     */
    public void addTokenToDenylist(String token) {
        String jti = extractJwtId(token);
        Date expiration = extractExpiration(token);
        
        if (jti != null && expiration != null) {
            long ttlSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            if (ttlSeconds > 0) {
                redisSessionService.addTokenToDenylist(jti, ttlSeconds);
                System.out.println("Added JWT ID " + jti + " to denylist with TTL " + ttlSeconds + " seconds");
            }
        }
    }

    /**
     * Check if token is on denylist
     */
    public boolean isTokenDenylisted(String token) {
        String jti = extractJwtId(token);
        return jti != null && redisSessionService.isTokenDenylisted(jti);
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

    // Legacy methods for compatibility
    public String generateToken(String email, String userId, String userType) {
        Set<User.Role> defaultRoles = Set.of(User.Role.CUSTOMER_VIEW);
        return generateToken(email, userId, userType, defaultRoles);
    }
}
