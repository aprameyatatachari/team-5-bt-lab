package com.login_module.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledService.class);
    
    @Autowired
    private AuthService authService;
    
    // Run every hour to cleanup expired sessions
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredSessions() {
        logger.info("Starting scheduled cleanup of expired sessions");
        try {
            authService.cleanupExpiredSessions();
            logger.info("Completed scheduled cleanup of expired sessions");
        } catch (Exception e) {
            logger.error("Error during scheduled session cleanup: {}", e.getMessage(), e);
        }
    }
}
