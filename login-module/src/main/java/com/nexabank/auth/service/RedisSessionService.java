package com.nexabank.auth.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class RedisSessionService {

    // Simple in-memory session storage (for demonstration)
    // In production, this should use Redis
    private final Map<String, LocalDateTime> userLockouts = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    public boolean isUserLockedOut(String userId) {
        LocalDateTime lockoutTime = userLockouts.get(userId);
        if (lockoutTime == null) {
            return false;
        }
        
        // Check if lockout has expired (10 minutes)
        if (LocalDateTime.now().isAfter(lockoutTime.plusMinutes(10))) {
            userLockouts.remove(userId);
            return false;
        }
        
        return true;
    }

    public void setUserLockout(String userId) {
        userLockouts.put(userId, LocalDateTime.now());
    }

    public void clearUserLockout(String userId) {
        userLockouts.remove(userId);
    }

    public void createSession(String sessionId, String userId) {
        userSessions.put(sessionId, userId);
    }

    public String getUserFromSession(String sessionId) {
        return userSessions.get(sessionId);
    }

    public void invalidateSession(String sessionId) {
        userSessions.remove(sessionId);
    }

    public boolean isValidSession(String sessionId) {
        return userSessions.containsKey(sessionId);
    }

    public long getRemainingLockoutTime(String userId) {
        LocalDateTime lockoutTime = userLockouts.get(userId);
        if (lockoutTime == null) {
            return 0;
        }
        
        LocalDateTime unlockTime = lockoutTime.plusMinutes(10);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(unlockTime)) {
            userLockouts.remove(userId);
            return 0;
        }
        
        // Return remaining seconds
        return java.time.Duration.between(now, unlockTime).getSeconds();
    }
}
