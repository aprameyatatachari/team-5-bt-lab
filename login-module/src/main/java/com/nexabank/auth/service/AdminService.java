package com.nexabank.auth.service;

import com.nexabank.auth.dto.CreateUserRequest;
import com.nexabank.auth.dto.UserDto;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserDto> getAllUsers(String userType, String status) {
        List<User> users;
        
        if (userType != null && status != null) {
            users = userRepository.findByUserTypeAndStatus(
                User.UserType.valueOf(userType.toUpperCase()),
                User.UserStatus.valueOf(status.toUpperCase())
            );
        } else if (userType != null) {
            users = userRepository.findByUserType(User.UserType.valueOf(userType.toUpperCase()));
        } else if (status != null) {
            users = userRepository.findByStatus(User.UserStatus.valueOf(status.toUpperCase()));
        } else {
            users = userRepository.findAll();
        }
        
        return users.stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(String userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            return new UserDto(user.get());
        }
        throw new RuntimeException("User not found with ID: " + userId);
    }

    public UserDto createUser(CreateUserRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists with email: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setUserType(request.getUserType());
        user.setStatus(User.UserStatus.ACTIVE);
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setCountry(request.getCountry());
        user.setPostalCode(request.getPostalCode());
        user.setAadharNumber(request.getAadharNumber());
        user.setPanNumber(request.getPanNumber());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return new UserDto(savedUser);
    }

    public UserDto updateUser(String userId, UserDto userDto) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        User user = optionalUser.get();
        
        // Update allowed fields
        if (userDto.getFirstName() != null) {
            user.setFirstName(userDto.getFirstName());
        }
        if (userDto.getLastName() != null) {
            user.setLastName(userDto.getLastName());
        }
        if (userDto.getPhoneNumber() != null) {
            user.setPhoneNumber(userDto.getPhoneNumber());
        }
        if (userDto.getDateOfBirth() != null) {
            user.setDateOfBirth(userDto.getDateOfBirth());
        }
        if (userDto.getAddress() != null) {
            user.setAddress(userDto.getAddress());
        }
        if (userDto.getCity() != null) {
            user.setCity(userDto.getCity());
        }
        if (userDto.getState() != null) {
            user.setState(userDto.getState());
        }
        if (userDto.getCountry() != null) {
            user.setCountry(userDto.getCountry());
        }
        if (userDto.getPostalCode() != null) {
            user.setPostalCode(userDto.getPostalCode());
        }
        if (userDto.getUserType() != null) {
            user.setUserType(userDto.getUserType());
        }
        
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return new UserDto(savedUser);
    }

    public UserDto updateUserStatus(String userId, String status) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        User user = optionalUser.get();
        user.setStatus(User.UserStatus.valueOf(status.toUpperCase()));
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return new UserDto(savedUser);
    }

    public void deleteUser(String userId) {
        Optional<User> user = userRepository.findById(userId);
        if (!user.isPresent()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        
        // Instead of hard delete, set status to INACTIVE
        User userToDelete = user.get();
        userToDelete.setStatus(User.UserStatus.INACTIVE);
        userToDelete.setUpdatedAt(LocalDateTime.now());
        userRepository.save(userToDelete);
    }

    public Map<String, Integer> getAdminStats() {
        Map<String, Integer> stats = new HashMap<>();
        
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByUserType(User.UserType.CUSTOMER);
        long totalAdmins = userRepository.countByUserType(User.UserType.ADMIN);
        long totalEmployees = userRepository.countByUserType(User.UserType.EMPLOYEE);
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long lockedUsers = userRepository.countByStatus(User.UserStatus.LOCKED);
        
        stats.put("totalUsers", (int) totalUsers);
        stats.put("totalCustomers", (int) totalCustomers);
        stats.put("totalAdmins", (int) totalAdmins);
        stats.put("totalEmployees", (int) totalEmployees);
        stats.put("activeUsers", (int) activeUsers);
        stats.put("lockedUsers", (int) lockedUsers);
        
        return stats;
    }
}
