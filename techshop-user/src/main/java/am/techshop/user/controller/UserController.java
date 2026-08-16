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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "Registration, authentication, and account management")
public class UserController {

    private final UserService userService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping("/api/users/register")
    @Operation(
            summary = "Register a new account",
            description = "Returns 409 if the email is already in use."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered", userService.register(request)));
    }

    @PostMapping("/api/users/login")
    @Operation(
            summary = "Authenticate and obtain a JWT",
            description = "Returns 401 for invalid credentials and 403 if email verification is required but not completed."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", userService.login(request)));
    }

    @GetMapping("/api/users")
    @Operation(summary = "List all users")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @GetMapping("/api/users/count")
    @Operation(summary = "Get the total number of registered users")
    public ResponseEntity<ApiResponse<Long>> getUserCount() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserCount()));
    }

    @GetMapping("/api/users/{id}")
    @Operation(
            summary = "Get a user by ID",
            description = "Accessible to admins, or via a valid internal service API key."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "ID of the user to fetch", required = true)
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
    @Operation(
            summary = "Verify an account via its email verification token",
            description = "Returns 400 if the token is invalid, already used, or expired."
    )
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @RequestParam String token,
            @RequestParam(required = false) Language lang) {
        return ResponseEntity.ok(ApiResponse.ok("Email verified", userService.verifyEmail(token, lang)));
    }

    @PostMapping("/api/users/resend-verification")
    @Operation(
            summary = "Resend the account verification email",
            description = "Always returns 200, whether or not the account exists, to avoid leaking account existence."
    )
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestBody @Valid ResendVerificationRequest request) {
        userService.resendVerification(request.email(), request.language());
        return ResponseEntity.ok(ApiResponse.ok(
                "If that account exists and isn't verified yet, a new verification email has been sent", null));
    }

    @PostMapping("/api/users/forgot-password")
    @Operation(
            summary = "Request a password reset email",
            description = "Always returns 200, whether or not the account exists, to avoid leaking account existence."
    )
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request.email(), request.language());
        return ResponseEntity.ok(ApiResponse.ok(
                "If that account exists, a password reset email has been sent", null));
    }

    @PostMapping("/api/users/reset-password")
    @Operation(
            summary = "Reset a password using a reset token",
            description = "Returns 400 if the token is invalid, already used, or expired."
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    @PutMapping("/api/users/{id}/role")
    @Operation(summary = "Update a user's role")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @Parameter(description = "ID of the user to update", required = true)
            @PathVariable Long id,
            @RequestBody @Valid RoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Role updated", userService.updateUserRole(id, request.role())));
    }

    @DeleteMapping("/api/users/me")
    @Operation(summary = "Delete the current authenticated user's account")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            Authentication authentication,
            @RequestBody @Valid DeleteAccountRequest request) {
        userService.deleteAccount(CurrentUser.id(authentication), request);
        return ResponseEntity.ok(ApiResponse.ok("Account deleted successfully", null));
    }

    @PutMapping("/api/users/me/password")
    @Operation(summary = "Change the current authenticated user's password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            Authentication authentication,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.id(authentication), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    @GetMapping("/api/users/me/preferences")
    @Operation(summary = "Get the current authenticated user's notification preferences")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> getMyNotificationPreferences(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getNotificationPreferences(CurrentUser.id(authentication))));
    }

    @PatchMapping("/api/users/me/preferences")
    @Operation(summary = "Update the current authenticated user's notification preferences")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> updateMyNotificationPreferences(
            Authentication authentication,
            @RequestBody @Valid NotificationPreferencesUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Preferences updated",
                userService.updateNotificationPreferences(CurrentUser.id(authentication), request.notifyPriceDrops())));
    }

    @GetMapping("/api/users/internal/price-drop-enabled")
    @Operation(
            summary = "Filter userIds to those opted in to price-drop notifications",
            description = "Internal service-to-service endpoint used by techshop-product, guarded by an internal API key."
    )
    public ResponseEntity<ApiResponse<List<Long>>> getPriceDropEnabledUserIds(
            @RequestParam List<Long> ids,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!isValidInternalKey(apiKey)) {
            throw new TechShopException("Forbidden", 403);
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.filterPriceDropEnabledUserIds(ids)));
    }

    @GetMapping("/api/users/internal/languages")
    @Operation(
            summary = "Get the preferred language for a set of userIds",
            description = "Internal service-to-service endpoint used to localize notifications, guarded by an internal API key."
    )
    public ResponseEntity<ApiResponse<List<UserLanguageResponse>>> getUserLanguages(
            @RequestParam List<Long> ids,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!isValidInternalKey(apiKey)) {
            throw new TechShopException("Forbidden", 403);
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserLanguages(ids)));
    }
}
