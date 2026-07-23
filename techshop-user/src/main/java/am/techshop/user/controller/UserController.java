package am.techshop.user.controller;

import am.techshop.common.dto.request.ForgotPasswordRequest;
import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResendVerificationRequest;
import am.techshop.common.dto.request.ResetPasswordRequest;
import am.techshop.common.dto.request.RoleUpdateRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @GetMapping("/api/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @GetMapping("/api/users/verify-email")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.ok("Email verified", userService.verifyEmail(token)));
    }

    @PostMapping("/api/users/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestBody @Valid ResendVerificationRequest request) {
        userService.resendVerification(request.email());
        return ResponseEntity.ok(ApiResponse.ok(
                "If that account exists and isn't verified yet, a new verification email has been sent", null));
    }

    @PostMapping("/api/users/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request.email());
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
}
