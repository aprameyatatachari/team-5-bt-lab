package com.nexabank.customer.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * JWT Validation Service for Customer Module
 * Fetches public key from Auth Service lazily on first use
 */
@Service
public class JwtValidationService {

    @Value("${auth.service.url:http://localhost:8080}")
    private String authServiceUrl;

    private volatile PublicKey publicKey;
    private final Object lock = new Object();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetch public key from Auth Service (lazy loading)
     */
    private void fetchPublicKeyFromAuthService() throws Exception {
        String publicKeyUrl = authServiceUrl + "/api/auth/public-key";
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(publicKeyUrl, Map.class);
            
            if (response != null && response.containsKey("publicKey")) {
                String publicKeyBase64 = (String) response.get("publicKey");
                byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.publicKey = keyFactory.generatePublic(keySpec);
                System.out.println("✅ Public key fetched from Auth Service successfully");
            } else {
                throw new Exception("Public key not found in response");
            }
        } catch (Exception e) {
            throw new Exception("Failed to fetch public key from " + publicKeyUrl + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get public key with lazy initialization (double-checked locking)
     */
    private PublicKey getPublicKey() throws Exception {
        if (publicKey == null) {
            synchronized (lock) {
                if (publicKey == null) {
                    fetchPublicKeyFromAuthService();
                }
            }
        }
        return publicKey;
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return true;
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract userId from token
     */
    public String extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract userType from token
     */
    public String extractUserType(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userType", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract roles from token
     */
    public List<String> extractRoles(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            String rolesString = claims.get("roles", String.class);
            if (rolesString != null && !rolesString.isEmpty()) {
                return Arrays.asList(rolesString.split(","));
            }
        } catch (Exception e) {
            // Ignore
        }
        return Collections.emptyList();
    }

    /**
     * Extract email/subject from token
     */
    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if token has specific role
     */
    public boolean hasRole(String token, String role) {
        List<String> roles = extractRoles(token);
        return roles.contains(role);
    }

    /**
     * Check if token has any of the specified roles
     */
    public boolean hasAnyRole(String token, String... requiredRoles) {
        List<String> tokenRoles = extractRoles(token);
        for (String role : requiredRoles) {
            if (tokenRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(String token) {
        String userType = extractUserType(token);
        return "ADMIN".equals(userType);
    }

    /**
     * Check if user is customer
     */
    public boolean isCustomer(String token) {
        String userType = extractUserType(token);
        return "CUSTOMER".equals(userType);
    }
}
