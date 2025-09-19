package com.nexabank.auth.util;

import com.nexabank.auth.entity.UserSession;
import com.nexabank.auth.repository.UserSessionRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Autowired
    private UserSessionRepository userSessionRepository;
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration}")
    private int jwtExpirationMs;
    
    @Value("${app.jwt.refresh-expiration}")
    private int refreshExpirationMs;
    
    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername());
    }
    
    public String generateTokenFromUsername(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("iat", new Date().getTime() / 1000); // Issued at time
        return createToken(claims, username, jwtExpirationMs);
    }
    
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("iat", new Date().getTime() / 1000); // Issued at time
        return createToken(claims, username, refreshExpirationMs);
    }
    
    private String createToken(Map<String, Object> claims, String subject, int expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String getUserNameFromJwtToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }
    
    public boolean validateJwtToken(String authToken) {
        try {
            // Parse and validate the token structure and signature
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken)
                    .getPayload();
            
            // Check if token is expired
            if (claims.getExpiration().before(new Date())) {
                logger.error("JWT token is expired");
                return false;
            }
            
            // Additional validation: Check if token exists in active sessions
            if (isAccessToken(authToken)) {
                return validateActiveSession(authToken);
            }
            
            return true;
            
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT token validation error: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Validate that the token corresponds to an active session in the database
     */
    private boolean validateActiveSession(String token) {
        try {
            UserSession session = userSessionRepository.findByAccessToken(token).orElse(null);
            if (session == null) {
                logger.error("Token not found in active sessions");
                return false;
            }
            
            if (!session.getIsActive()) {
                logger.error("Session is not active");
                return false;
            }
            
            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                logger.error("Session has expired");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error validating active session: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
    
    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return (String) claims.get("type");
        } catch (Exception e) {
            logger.error("Error extracting token type: {}", e.getMessage());
            return null;
        }
    }
    
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            logger.error("Error extracting expiration date: {}", e.getMessage());
            return null;
        }
    }
    
    public Date getIssuedAtDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getIssuedAt();
        } catch (Exception e) {
            logger.error("Error extracting issued at date: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if the token is an access token (not refresh token)
     */
    private boolean isAccessToken(String token) {
        String tokenType = getTokenType(token);
        return "access".equals(tokenType);
    }
    
    /**
     * Invalidate a token by marking its session as inactive
     */
    public boolean invalidateToken(String token) {
        try {
            UserSession session = userSessionRepository.findByAccessToken(token).orElse(null);
            if (session != null) {
                session.setIsActive(false);
                userSessionRepository.save(session);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error invalidating token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get additional claims from token for enhanced validation
     */
    public Map<String, Object> getAllClaimsFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new HashMap<>(claims);
        } catch (Exception e) {
            logger.error("Error extracting claims from token: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
