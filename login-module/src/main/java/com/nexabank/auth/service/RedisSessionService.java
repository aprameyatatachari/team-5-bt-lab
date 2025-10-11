package com.nexabank.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class RedisSessionService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String LOCKOUT_PREFIX = "lockout:";
    private static final String SESSION_PREFIX = "session:";
    private static final String DENYLIST_PREFIX = "denylist:";
    private static final int LOCKOUT_DURATION_MINUTES = 10;

    public boolean isUserLockedOut(String userId) {
        try {
            String lockoutKey = LOCKOUT_PREFIX + userId;
            String lockoutTime = redisTemplate.opsForValue().get(lockoutKey);
            return lockoutTime != null; // If key exists, user is locked out
        } catch (Exception e) {
            // Fallback to false if Redis is unavailable
            System.err.println("Redis unavailable for lockout check: " + e.getMessage());
            return false;
        }
    }

    public void createSession(String sessionId, String userId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            // Sessions expire after 24 hours
            redisTemplate.opsForValue().set(sessionKey, userId, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            System.err.println("Failed to create session in Redis: " + e.getMessage());
        }
    }

    public String getUserFromSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            return redisTemplate.opsForValue().get(sessionKey);
        } catch (Exception e) {
            System.err.println("Failed to get user from session in Redis: " + e.getMessage());
            return null;
        }
    }

    public void invalidateSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
        } catch (Exception e) {
            System.err.println("Failed to invalidate session in Redis: " + e.getMessage());
        }
    }

    public boolean isValidSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            return redisTemplate.hasKey(sessionKey);
        } catch (Exception e) {
            System.err.println("Failed to check session validity in Redis: " + e.getMessage());
            return false;
        }
    }

    public long getRemainingLockoutTime(String userId) {
        try {
            String lockoutKey = LOCKOUT_PREFIX + userId;
            Long ttl = redisTemplate.getExpire(lockoutKey, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            System.err.println("Failed to get remaining lockout time from Redis: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Health check method to verify Redis connectivity
     */
    public boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().set("health-check", "ping", 1, TimeUnit.SECONDS);
            String response = redisTemplate.opsForValue().get("health-check");
            return "ping".equals(response);
        } catch (Exception e) {
            return false;
        }
    }

    // ========== JWT DENYLIST METHODS ==========
    
    /**
     * Add JWT ID to denylist (for immediate logout)
     */
    public void addTokenToDenylist(String jti, long ttlSeconds) {
        try {
            String denylistKey = DENYLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(denylistKey, "denied", ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Failed to add JWT to denylist in Redis: " + e.getMessage());
        }
    }

    /**
     * Check if JWT ID is on denylist
     */
    public boolean isTokenDenylisted(String jti) {
        try {
            String denylistKey = DENYLIST_PREFIX + jti;
            return redisTemplate.hasKey(denylistKey);
        } catch (Exception e) {
            System.err.println("Failed to check JWT denylist in Redis: " + e.getMessage());
            return false; // Fail open - allow access if Redis is down
        }
    }

    // ========== USER LOCKOUT METHODS (Enhanced for Bank-Style Session Control) ==========
    
    /**
     * Set user lockout for 10 minutes (prevents new logins)
     */
    public void setUserLockout(String userId) {
        try {
            String lockoutKey = LOCKOUT_PREFIX + userId;
            String lockoutTime = LocalDateTime.now().toString();
            
            // Set with automatic expiration after 10 minutes
            redisTemplate.opsForValue().set(lockoutKey, lockoutTime, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
            System.out.println("User " + userId + " locked out for " + LOCKOUT_DURATION_MINUTES + " minutes");
        } catch (Exception e) {
            System.err.println("Failed to set user lockout in Redis: " + e.getMessage());
        }
    }

    /**
     * Clear user lockout (allows immediate re-login)
     */
    public void clearUserLockout(String userId) {
        try {
            String lockoutKey = LOCKOUT_PREFIX + userId;
            redisTemplate.delete(lockoutKey);
            System.out.println("User " + userId + " lockout cleared - can login immediately");
        } catch (Exception e) {
            System.err.println("Failed to clear user lockout in Redis: " + e.getMessage());
        }
    }
}
