package com.nexabank.auth.config;

import com.nexabank.auth.service.RedisSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements CommandLineRunner {

    @Autowired
    private RedisSessionService redisSessionService;

    @Override
    public void run(String... args) throws Exception {
        try {
            if (redisSessionService.isRedisAvailable()) {
                System.out.println("✅ Redis connection successful - Session management using Redis");
            } else {
                System.err.println("⚠️  WARNING: Redis connection failed - Session management will not work properly");
                System.err.println("📋 To fix: Install and start Redis server on localhost:6379");
                System.err.println("💡 For Windows: Download Redis from https://github.com/microsoftarchive/redis/releases");
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR: Redis health check failed: " + e.getMessage());
            System.err.println("📋 To fix: Install and start Redis server on localhost:6379");
        }
    }

    public boolean isRedisHealthy() {
        return redisSessionService.isRedisAvailable();
    }
}