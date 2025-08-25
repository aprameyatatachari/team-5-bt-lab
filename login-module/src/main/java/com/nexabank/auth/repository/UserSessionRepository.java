package com.nexabank.auth.repository;

import com.nexabank.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    Optional<UserSession> findByAccessToken(String accessToken);
    
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    List<UserSession> findByUserUserIdAndIsActiveTrue(String userId);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.accessToken = :accessToken")
    void deactivateByAccessToken(@Param("accessToken") String accessToken);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.user.userId = :userId")
    void deactivateAllByUserId(@Param("userId") String userId);
    
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :currentTime OR us.isActive = false")
    void deleteExpiredSessions(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.userId = :userId AND us.isActive = true")
    long countActiveSessionsByUserId(@Param("userId") String userId);
    
    List<UserSession> findByUserUserIdOrderByLastAccessedDesc(String userId);
}
