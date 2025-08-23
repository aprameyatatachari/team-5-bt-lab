package com.login_module.repository;

import com.login_module.entity.User;
import com.login_module.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    Optional<UserSession> findByAccessTokenAndIsActiveTrue(String accessToken);
    
    Optional<UserSession> findByRefreshTokenAndIsActiveTrue(String refreshToken);
    
    List<UserSession> findByUserAndIsActiveTrue(User user);
    
    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.isActive = true")
    List<UserSession> findActiveSessionsByUser(@Param("user") User user);
    
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.isActive = true")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    void deleteByUserAndIsActiveTrue(User user);
}
