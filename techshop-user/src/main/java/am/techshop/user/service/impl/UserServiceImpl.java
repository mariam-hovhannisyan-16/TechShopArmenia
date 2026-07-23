package am.techshop.user.service.impl;

import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResetPasswordRequest;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.UserRole;
import am.techshop.common.event.PasswordResetRequestedEvent;
import am.techshop.common.event.UserRegisteredEvent;
import am.techshop.common.event.UserVerifiedEvent;
import am.techshop.common.exception.TechShopException;
import am.techshop.common.exception.UserNotFoundException;
import am.techshop.user.entity.User;
import am.techshop.user.kafka.UserEventProducer;
import am.techshop.user.mapper.UserMapper;
import am.techshop.user.repository.UserRepository;
import am.techshop.user.service.JwtService;
import am.techshop.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;
    private static final long RESET_TOKEN_TTL_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserEventProducer userEventProducer;

    @Value("${app.require-email-verification:false}")
    private boolean requireEmailVerification;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new TechShopException("Email already in use", 409);
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role() != null ? request.role() : UserRole.CUSTOMER)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_TTL_HOURS))
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        // Sending the verification email happens asynchronously in
        // techshop-notification (consuming this event) — a mail failure
        // there can never fail this request, and the user can always
        // request a resend via /api/users/resend-verification.
        userEventProducer.sendUserRegisteredEvent(
                new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail(), savedUser.getName(), verificationToken)
        );

        return new AuthResponse(token, userMapper.toResponse(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> UserNotFoundException.byEmail(request.email()));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new TechShopException("Invalid email or password", 401);
        }

        if (requireEmailVerification && !user.isEmailVerified()) {
            throw new TechShopException("Please verify your email before logging in", 403);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, userMapper.toResponse(user));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new TechShopException("Invalid or already-used verification link", 400));

        if (user.isEmailVerified()) {
            throw new TechShopException("This account is already verified", 409);
        }

        if (user.getVerificationTokenExpiresAt() == null || user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TechShopException("This verification link has expired. Please request a new one.", 400);
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        User savedUser = userRepository.save(user);

        userEventProducer.sendUserVerifiedEvent(
                new UserVerifiedEvent(savedUser.getId(), savedUser.getEmail(), savedUser.getName())
        );

        return userMapper.toResponse(savedUser);
    }

    // Always succeeds from the caller's point of view whether or not the
    // email exists / is already verified, so this endpoint can't be used to
    // enumerate registered addresses.
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }

            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_TTL_HOURS));
            userRepository.save(user);

            userEventProducer.sendUserRegisteredEvent(
                    new UserRegisteredEvent(user.getId(), user.getEmail(), user.getName(), verificationToken)
            );
        });
    }

    // Always succeeds from the caller's point of view whether or not the
    // email exists, so this endpoint can't be used to enumerate registered
    // addresses.
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetToken(resetToken);
            user.setResetTokenExpiresAt(LocalDateTime.now().plusHours(RESET_TOKEN_TTL_HOURS));
            userRepository.save(user);

            userEventProducer.sendPasswordResetRequestedEvent(
                    new PasswordResetRequestedEvent(user.getId(), user.getEmail(), user.getName(), resetToken)
            );
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new TechShopException("Invalid or already-used reset link", 400));

        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TechShopException("This reset link has expired. Please request a new one.", 400);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setRole(role);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }
}
