package com.nexabank.auth.security;

import com.nexabank.auth.service.JwtTokenService;
import com.nexabank.auth.service.UserDetailsServiceImpl;
import com.nexabank.auth.service.RedisSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private RedisSessionService redisSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                // ENHANCED VALIDATION: Check both JWT validity AND denylist
                if (jwtTokenService.validateToken(jwt)) {
                    username = jwtTokenService.extractEmail(jwt);
                    
                    // Additional check: Verify token is not on denylist
                    if (jwtTokenService.isTokenDenylisted(jwt)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\":\"Token has been invalidated. Please login again.\"}");
                        response.setContentType("application/json");
                        System.out.println("🚫 DENYLIST BLOCK: JWT rejected (user logged out)");
                        return;
                    }
                    
                    // Check if user is locked out (prevents new sessions but allows existing valid ones)
                    if (username != null && redisSessionService.isUserLockedOut(username)) {
                        // Note: This check is primarily for new login attempts
                        // Existing valid sessions should continue to work
                        String jti = jwtTokenService.extractJwtId(jwt);
                        if (jti != null && !redisSessionService.isValidSession(jti)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"error\":\"Account locked. Please login again.\"}");
                            response.setContentType("application/json");
                            System.out.println("🔒 LOCKOUT BLOCK: User locked and no valid session");
                            return;
                        }
                    }
                } else {
                    // Token validation failed (expired, invalid signature, etc.)
                    username = null;
                }
            } catch (Exception e) {
                logger.error("JWT token validation failed: " + e.getMessage());
                username = null;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Double-check token validity before setting authentication
            if (jwtTokenService.validateToken(jwt)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                // Log successful authentication
                String jti = jwtTokenService.extractJwtId(jwt);
                System.out.println("✅ AUTH SUCCESS: User " + username + " authenticated with JTI: " + jti);
            }
        }

        filterChain.doFilter(request, response);
    }
}
