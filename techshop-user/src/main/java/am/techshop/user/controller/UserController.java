package am.techshop.user.controller;

import am.techshop.common.dto.request.ChangePasswordRequest;
import am.techshop.common.dto.request.DeleteAccountRequest;
import am.techshop.common.dto.request.ForgotPasswordRequest;
import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.NotificationPreferencesUpdateRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResendVerificationRequest;
import am.techshop.common.dto.request.ResetPasswordRequest;
import am.techshop.common.dto.request.RoleUpdateRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.NotificationPreferencesResponse;
import am.techshop.common.dto.response.UserLanguageResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.exception.TechShopException;
import am.techshop.common.security.CurrentUser;
import am.techshop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping("/api/users/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered", userService.register(request)));
    }

    @PostMapping("/api/users/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", userService.login(request)));
    }

    @GetMapping("/api/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @GetMapping("/api/users/count")
    public ResponseEntity<ApiResponse<Long>> getUserCount() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserCount()));
    }

    @GetMapping("/api/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id,
            Authentication authentication,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!isValidInternalKey(apiKey) && !isAdmin(authentication)) {
            throw new TechShopException("You do not have permission to access this resource", 403);
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    private boolean isValidInternalKey(String providedKey) {
        return providedKey != null && MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    @GetMapping("/api/users/verify-email")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @RequestParam String token,
            @RequestParam(required = false) Language lang) {
        return ResponseEntity.ok(ApiResponse.ok("Email verified", userService.verifyEmail(token, lang)));
    }

    @PostMapping("/api/users/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestBody @Valid ResendVerificationRequest request) {
        userService.resendVerification(request.email(), request.language());
        return ResponseEntity.ok(ApiResponse.ok(
                "If that account exists and isn't verified yet, a new verification email has been sent", null));
    }

    @PostMapping("/api/users/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request.email(), request.language());
        return ResponseEntity.ok(ApiResponse.ok(
                "If that account exists, a password reset email has been sent", null));
    }

    @PostMapping("/api/users/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    @PutMapping("/api/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @RequestBody @Valid RoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Role updated", userService.updateUserRole(id, request.role())));
    }

    @DeleteMapping("/api/users/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            Authentication authentication,
            @RequestBody @Valid DeleteAccountRequest request) {
        userService.deleteAccount(CurrentUser.id(authentication), request);
        return ResponseEntity.ok(ApiResponse.ok("Account deleted successfully", null));
    }

    @PutMapping("/api/users/me/password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            Authentication authentication,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.id(authentication), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    @GetMapping("/api/users/me/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> getMyNotificationPreferences(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getNotificationPreferences(CurrentUser.id(authentication))));
    }

    @PatchMapping("/api/users/me/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> updateMyNotificationPreferences(
            Authentication authentication,
            @RequestBody @Valid NotificationPreferencesUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Preferences updated",
                userService.updateNotificationPreferences(CurrentUser.id(authentication), request.notifyPriceDrops())));
    }

    @GetMapping("/api/users/internal/price-drop-enabled")
    public ResponseEntity<ApiResponse<List<Long>>> getPriceDropEnabledUserIds(
            @RequestParam List<Long> ids,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!isValidInternalKey(apiKey)) {
            throw new TechShopException("Forbidden", 403);
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.filterPriceDropEnabledUserIds(ids)));
    }

    @GetMapping("/api/users/internal/languages")
    public ResponseEntity<ApiResponse<List<UserLanguageResponse>>> getUserLanguages(
            @RequestParam List<Long> ids,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!isValidInternalKey(apiKey)) {
            throw new TechShopException("Forbidden", 403);
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserLanguages(ids)));
    }
}
