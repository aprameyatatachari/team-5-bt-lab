package com.nexabank.customer.repository;

import com.nexabank.customer.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    Optional<UserSession> findByUserIdAndAccessToken(String userId, String accessToken);
    
    Optional<UserSession> findByUserIdAndRefreshToken(String userId, String refreshToken);
    
    List<UserSession> findByUserId(String userId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
    
    @Modifying
    @Transactional
    void deleteByUserIdAndAccessToken(String userId, String accessToken);
    
    @Modifying
    @Transactional
    void deleteByUserIdAndRefreshToken(String userId, String refreshToken);
    
    boolean existsByUserId(String userId);
}
