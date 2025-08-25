package com.nexabank.auth.repository;

import com.nexabank.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    boolean existsByAadharNumber(String aadharNumber);
    
    boolean existsByPanNumber(String panNumber);
    
    List<User> findByUserType(User.UserType userType);
    
    List<User> findByStatus(User.UserStatus status);
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.userType = :userType")
    List<User> findByStatusAndUserType(@Param("status") User.UserStatus status, 
                                      @Param("userType") User.UserType userType);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = :userType")
    long countByUserType(@Param("userType") User.UserType userType);
    
    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name% OR u.email LIKE %:name%")
    List<User> findByNameOrEmail(@Param("name") String name);
    
    List<User> findByUserTypeAndStatus(User.UserType userType, User.UserStatus status);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") User.UserStatus status);
}
