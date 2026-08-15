package am.techshop.user.service;

import am.techshop.common.dto.request.ChangePasswordRequest;
import am.techshop.common.dto.request.DeleteAccountRequest;
import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResetPasswordRequest;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.NotificationPreferencesResponse;
import am.techshop.common.dto.response.UserLanguageResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.enums.UserRole;

import java.util.List;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    List<UserResponse> getAllUsers();
    long getUserCount();
    UserResponse getUserById(Long id);
    UserResponse verifyEmail(String token, Language language);
    void resendVerification(String email, Language language);
    void forgotPassword(String email, Language language);
    void resetPassword(ResetPasswordRequest request);
    UserResponse updateUserRole(Long id, UserRole role);
    void deleteAccount(Long userId, DeleteAccountRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    NotificationPreferencesResponse getNotificationPreferences(Long userId);
    NotificationPreferencesResponse updateNotificationPreferences(Long userId, boolean notifyPriceDrops);
    List<Long> filterPriceDropEnabledUserIds(List<Long> userIds);
    List<UserLanguageResponse> getUserLanguages(List<Long> userIds);
}