package com.nexabank.customer.security;

import com.nexabank.customer.service.JwtValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for Customer Module
 * Validates JWT tokens from Auth Service and sets authentication context
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtValidationService jwtValidationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Validate token and extract claims
                if (jwtValidationService.validateToken(token)) {
                    String userId = jwtValidationService.extractUserId(token);
                    String userType = jwtValidationService.extractUserType(token);
                    List<String> roles = jwtValidationService.extractRoles(token);
                    
                    // Create authorities from roles
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set custom attributes for easy access
                    request.setAttribute("userId", userId);
                    request.setAttribute("userType", userType);
                    request.setAttribute("roles", roles);
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    System.out.println("✅ JWT Authentication successful for userId: " + userId + ", userType: " + userType);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ JWT Authentication failed: " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
