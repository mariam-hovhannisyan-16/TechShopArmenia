package am.techshop.user.service;

import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResetPasswordRequest;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.UserRole;

import java.util.List;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    List<UserResponse> getAllUsers();
    long getUserCount();
    UserResponse getUserById(Long id);
    UserResponse verifyEmail(String token);
    void resendVerification(String email);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    UserResponse updateUserRole(Long id, UserRole role);
}