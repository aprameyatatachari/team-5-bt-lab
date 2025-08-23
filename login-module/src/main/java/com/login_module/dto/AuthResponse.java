package com.login_module.dto;

import com.login_module.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfo user;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String userId;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private User.UserType userType;
        private User.UserStatus status;
        private LocalDateTime lastLogin;
        
        // Masked PII data
        private String maskedAadhar;
        private String maskedPan;
        
        public static UserInfo fromUser(User user) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setEmail(user.getEmail());
            userInfo.setFirstName(user.getFirstName());
            userInfo.setLastName(user.getLastName());
            userInfo.setPhoneNumber(user.getPhoneNumber());
            userInfo.setUserType(user.getUserType());
            userInfo.setStatus(user.getStatus());
            userInfo.setLastLogin(user.getLastLogin());
            
            // Mask PII data
            if (user.getAadharNumber() != null) {
                userInfo.setMaskedAadhar(maskAadhar(user.getAadharNumber()));
            }
            if (user.getPanNumber() != null) {
                userInfo.setMaskedPan(maskPan(user.getPanNumber()));
            }
            
            return userInfo;
        }
        
        private static String maskAadhar(String aadhar) {
            if (aadhar == null || aadhar.length() < 4) {
                return "****";
            }
            return "XXXX-XXXX-" + aadhar.substring(aadhar.length() - 4);
        }
        
        private static String maskPan(String pan) {
            if (pan == null || pan.length() < 4) {
                return "****";
            }
            return "XXXXX" + pan.substring(pan.length() - 4);
        }
    }
}
