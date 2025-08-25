package com.nexabank.auth.service;

import com.nexabank.auth.entity.User;
import com.nexabank.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("User account is not active: " + email);
        }
        
        return UserPrincipal.create(user);
    }
    
    public static class UserPrincipal implements UserDetails {
        private String id;
        private String email;
        private String password;
        private User.UserType userType;
        private User.UserStatus status;
        
        public UserPrincipal(String id, String email, String password, User.UserType userType, User.UserStatus status) {
            this.id = id;
            this.email = email;
            this.password = password;
            this.userType = userType;
            this.status = status;
        }
        
        public static UserPrincipal create(User user) {
            return new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getUserType(),
                user.getStatus()
            );
        }
        
        public String getId() {
            return id;
        }
        
        public User.UserType getUserType() {
            return userType;
        }
        
        @Override
        public String getUsername() {
            return email;
        }
        
        @Override
        public String getPassword() {
            return password;
        }
        
        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userType.name()));
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return status != User.UserStatus.LOCKED;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            return status == User.UserStatus.ACTIVE;
        }
    }
}
