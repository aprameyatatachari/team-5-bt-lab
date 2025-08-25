package com.nexabank.customer.repository;

import com.nexabank.customer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findById(String userId);
    
    Optional<User> findByEmail(String email);
}
