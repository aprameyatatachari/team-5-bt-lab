package com.login_module.service;

import com.login_module.entity.User;
import com.login_module.exception.BadRequestException;
import com.login_module.exception.UnauthorizedException;
import com.login_module.repository.BankAccountRepository;
import com.login_module.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BankAccountRepository bankAccountRepository;
    
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Mask sensitive information
        users.forEach(this::maskSensitiveInfo);
        return users;
    }
    
    public User getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        maskSensitiveInfo(user);
        return user;
    }
    
    @Transactional
    public User updateUser(String userId, User updatedUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        // Update allowed fields (don't update sensitive fields like password)
        if (updatedUser.getFirstName() != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            existingUser.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        }
        if (updatedUser.getAddress() != null) {
            existingUser.setAddress(updatedUser.getAddress());
        }
        if (updatedUser.getCity() != null) {
            existingUser.setCity(updatedUser.getCity());
        }
        if (updatedUser.getState() != null) {
            existingUser.setState(updatedUser.getState());
        }
        if (updatedUser.getCountry() != null) {
            existingUser.setCountry(updatedUser.getCountry());
        }
        if (updatedUser.getPostalCode() != null) {
            existingUser.setPostalCode(updatedUser.getPostalCode());
        }
        if (updatedUser.getUserType() != null) {
            existingUser.setUserType(updatedUser.getUserType());
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(existingUser);
        maskSensitiveInfo(savedUser);
        return savedUser;
    }
    
    @Transactional
    public User updateUserStatus(String userId, User.UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        
        // Reset failed login attempts if activating
        if (status == User.UserStatus.ACTIVE) {
            user.setFailedLoginAttempts(0);
        }
        
        User savedUser = userRepository.save(user);
        maskSensitiveInfo(savedUser);
        return savedUser;
    }
    
    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        // Don't allow deleting admin users
        if (user.getUserType() == User.UserType.ADMIN) {
            throw new BadRequestException("Cannot delete admin users");
        }
        
        // Soft delete by setting status to INACTIVE
        user.setStatus(User.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    public Map<String, Object> getBankStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        Long totalUsers = userRepository.count();
        Long totalCustomers = userRepository.countByUserType(User.UserType.CUSTOMER);
        Long totalAdmins = userRepository.countByUserType(User.UserType.ADMIN);
        Long totalEmployees = userRepository.countByUserType(User.UserType.EMPLOYEE);
        
        stats.put("totalUsers", totalUsers);
        stats.put("totalCustomers", totalCustomers);
        stats.put("totalAdmins", totalAdmins);
        stats.put("totalEmployees", totalEmployees);
        
        // Account statistics
        Long totalAccounts = bankAccountRepository.count();
        Double totalBalance = bankAccountRepository.getTotalBalance();
        
        stats.put("totalAccounts", totalAccounts);
        stats.put("totalBalance", totalBalance != null ? totalBalance : 0.0);
        stats.put("pendingApprovals", 0); // Placeholder for future functionality
        
        return stats;
    }
    
    private void maskSensitiveInfo(User user) {
        // Clear password hash
        user.setPasswordHash(null);
        
        // Mask Aadhar number if present
        if (user.getAadharNumber() != null && user.getAadharNumber().length() >= 8) {
            String aadhar = user.getAadharNumber();
            user.setAadharNumber("****" + aadhar.substring(aadhar.length() - 4));
        }
        
        // Mask PAN number if present
        if (user.getPanNumber() != null && user.getPanNumber().length() >= 6) {
            String pan = user.getPanNumber();
            user.setPanNumber("****" + pan.substring(pan.length() - 2));
        }
    }
}
