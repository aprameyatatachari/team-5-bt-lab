package com.nexabank.customer.security;

import com.nexabank.customer.repository.UserRepository;
import com.nexabank.customer.repository.UserSessionRepository;
import com.nexabank.customer.entity.User;
import com.nexabank.customer.entity.UserSession;
import com.nexabank.customer.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // Extract email from token subject (login module format)
                String email = jwtUtils.getUserIdFromJwtToken(jwt); // This actually returns the subject, which is email
                
                if (email != null) {
                    log.debug("Processing JWT for email: {}", email);
                    
                    // Find user by email in customer module database
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isEmpty()) {
                        log.warn("User not found with email: {}", email);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    User user = userOpt.get();
                    if (!"ACTIVE".equals(user.getStatus())) {
                        log.warn("User account is not active: {}", email);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Check if session exists and is valid
                    Optional<UserSession> sessionOpt = userSessionRepository.findByUserIdAndAccessToken(user.getUserId(), jwt);
                    if (sessionOpt.isEmpty()) {
                        log.warn("Session not found for user: {}", email);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UserSession session = sessionOpt.get();
                    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                        // Clean up expired session
                        userSessionRepository.deleteByUserIdAndAccessToken(user.getUserId(), jwt);
                        log.warn("Session expired for user: {}", email);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Update session last accessed time
                    session.setUpdatedAt(LocalDateTime.now());
                    userSessionRepository.save(session);

                    // Create authentication object using user type from database
                    String userType = user.getUserType() != null ? user.getUserType() : "CUSTOMER";
                    List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase())
                    );
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(user.getUserId(), null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Successfully authenticated user: {} with role: {}", email, userType);
                } else {
                    log.warn("Could not extract email from JWT token");
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT filtering for public endpoints
        return path.equals("/api/auth/login") || 
               path.equals("/api/auth/register") ||
               path.equals("/api/auth/refresh") ||
               path.startsWith("/error") ||
               path.equals("/");
    }
}
