package com.nexabank.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "auth_users")
public class User extends AuditLoggable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private String userId;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType = UserType.CUSTOMER;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

    public enum UserType {
        CUSTOMER, ADMIN, EMPLOYEE, SYSTEM
    }

    public enum Role {
        // Customer roles
        CUSTOMER_VIEW, CUSTOMER_TRANSACTION, 
        
        // Employee roles  
        EMPLOYEE_VIEW, EMPLOYEE_CUSTOMER_MANAGEMENT,
        
        // Admin roles
        ADMIN_VIEW, ADMIN_USER_MANAGEMENT, ADMIN_SYSTEM_CONFIG,
        ADMIN_REPORTS, ADMIN_FULL_ACCESS,
        
        // System roles
        SYSTEM_API_ACCESS
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, LOCKED, SUSPENDED, PENDING_ACTIVATION
    }

    // Constructors
    public User() {}

    public User(String email, String passwordHash, UserType userType) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.passwordChangedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { 
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }
    
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }
    
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    
    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public LocalDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }

    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    // Utility methods for role checking
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(Role... roles) {
        if (this.roles == null) return false;
        for (Role role : roles) {
            if (this.roles.contains(role)) return true;
        }
        return false;
    }

    public boolean isAdmin() {
        return userType == UserType.ADMIN || hasAnyRole(Role.ADMIN_FULL_ACCESS, Role.ADMIN_USER_MANAGEMENT);
    }

    public boolean isCustomer() {
        return userType == UserType.CUSTOMER;
    }

    public boolean isEmployee() {
        return userType == UserType.EMPLOYEE;
    }

    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null) {
            setCreatedAt(LocalDateTime.now());
        }
        if (passwordChangedAt == null) {
            passwordChangedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Update timestamp is handled by @UpdateTimestamp in AuditLoggable
    }
}