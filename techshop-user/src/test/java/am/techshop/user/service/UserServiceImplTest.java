package am.techshop.user.service;

import am.techshop.common.dto.request.DeleteAccountRequest;
import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResetPasswordRequest;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.UserRole;
import am.techshop.common.event.PasswordResetRequestedEvent;
import am.techshop.common.event.UserDeletedEvent;
import am.techshop.common.event.UserRegisteredEvent;
import am.techshop.common.event.UserVerifiedEvent;
import am.techshop.common.exception.TechShopException;
import am.techshop.common.exception.UserNotFoundException;
import am.techshop.user.config.InternalProperties;
import am.techshop.user.entity.User;
import am.techshop.user.kafka.UserEventProducer;
import am.techshop.user.mapper.UserMapper;
import am.techshop.user.repository.UserRepository;
import am.techshop.user.security.JwtService;
import am.techshop.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventProducer userEventProducer;

    @Mock
    private InternalProperties internalProperties;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_WhenEmailNotTaken_RegistersUser() {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", null);
        User newUser = new User();
        newUser.setName("Mariam");
        newUser.setEmail("mariam@test.com");
        newUser.setPassword("encoded");
        newUser.setRole(UserRole.CUSTOMER);
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Mariam");
        savedUser.setEmail("mariam@test.com");
        savedUser.setPassword("encoded");
        savedUser.setRole(UserRole.CUSTOMER);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userMapper.toEntity(eq(request), eq("encoded"), eq(UserRole.CUSTOMER), any(), any())).thenReturn(newUser);
        when(userRepository.save(newUser)).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        AuthResponse result = userService.register(request);

        assertNotNull(result);
        assertEquals("token", result.token());
        verify(userEventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void register_WhenRoleNotProvided_DefaultsToCustomer() {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", null);
        User newUser = new User();
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Mariam");
        savedUser.setEmail("mariam@test.com");
        savedUser.setRole(UserRole.CUSTOMER);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userMapper.toEntity(eq(request), eq("encoded"), eq(UserRole.CUSTOMER), any(), any())).thenReturn(newUser);
        when(userRepository.save(newUser)).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        userService.register(request);

        verify(userMapper).toEntity(eq(request), eq("encoded"), eq(UserRole.CUSTOMER), any(), any());
    }

    @Test
    void register_WhenRoleProvided_UsesRequestedRole() {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", UserRole.ADMIN);
        User newUser = new User();
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Mariam");
        savedUser.setEmail("mariam@test.com");
        savedUser.setRole(UserRole.ADMIN);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.ADMIN, LocalDateTime.now(), true);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userMapper.toEntity(eq(request), eq("encoded"), eq(UserRole.ADMIN), any(), any())).thenReturn(newUser);
        when(userRepository.save(newUser)).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        userService.register(request);

        verify(userMapper).toEntity(eq(request), eq("encoded"), eq(UserRole.ADMIN), any(), any());
    }

    @Test
    void register_WhenEventPublishFails_StillReturnsAuthResponse() {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", UserRole.ADMIN);
        User newUser = new User();
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Mariam");
        savedUser.setEmail("mariam@test.com");
        savedUser.setRole(UserRole.ADMIN);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.ADMIN, LocalDateTime.now(), true);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userMapper.toEntity(eq(request), eq("encoded"), eq(UserRole.ADMIN), any(), any())).thenReturn(newUser);
        when(userRepository.save(newUser)).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);
        doThrow(new RuntimeException("kafka unavailable"))
                .when(userEventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));

        AuthResponse result = userService.register(request);

        assertNotNull(result);
        assertEquals("token", result.token());
        assertEquals(UserRole.ADMIN, result.user().role());
    }

    @Test
    void register_WhenEmailAlreadyTaken_ThrowsException() {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", null);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.register(request));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void login_WhenValidCredentials_ReturnsToken() {
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        User user = new User();
        user.setId(1L);
        user.setEmail("mariam@test.com");
        user.setPassword("encoded");
        user.setRole(UserRole.CUSTOMER);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse result = userService.login(request);

        assertNotNull(result);
        assertEquals("token", result.token());
    }

    @Test
    void login_WhenUserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.login(request));
    }

    @Test
    void login_WhenWrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("mariam@test.com", "wrongpassword");
        User user = new User();
        user.setId(1L);
        user.setEmail("mariam@test.com");
        user.setPassword("encoded");
        user.setRole(UserRole.CUSTOMER);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.login(request));

        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void getUserById_WhenExists_ReturnsUser() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setName("Mariam");
        UserResponse userResponse = new UserResponse(id, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(id);

        assertNotNull(result);
        assertEquals(id, result.id());
    }

    @Test
    void getUserById_WhenNotFound_ThrowsException() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(id));
    }

    @Test
    void updateUserRole_WhenExists_UpdatesRole() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setName("Mariam");
        user.setRole(UserRole.CUSTOMER);
        UserResponse userResponse = new UserResponse(id, "Mariam", "mariam@test.com", UserRole.ADMIN, LocalDateTime.now(), true);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUserRole(id, UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(UserRole.ADMIN, result.role());
    }

    @Test
    void updateUserRole_WhenNotFound_ThrowsException() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateUserRole(id, UserRole.ADMIN));
    }

    @Test
    void login_WhenUnverifiedAndVerificationRequired_ThrowsException() {
        when(internalProperties.requireEmailVerification()).thenReturn(true);
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        User user = new User();
        user.setId(1L);
        user.setEmail("mariam@test.com");
        user.setPassword("encoded");
        user.setRole(UserRole.CUSTOMER);
        user.setEmailVerified(false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.login(request));

        assertEquals(403, ex.getStatusCode());
    }

    @Test
    void login_WhenUnverifiedButVerificationNotRequired_Succeeds() {
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        User user = new User();
        user.setId(1L);
        user.setEmail("mariam@test.com");
        user.setPassword("encoded");
        user.setRole(UserRole.CUSTOMER);
        user.setEmailVerified(false);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse result = userService.login(request);

        assertNotNull(result);
        assertFalse(result.user().emailVerified());
    }

    @Test
    void verifyEmail_WhenTokenValid_MarksUserVerified() {
        User user = new User();
        user.setId(1L);
        user.setName("Mariam");
        user.setEmail("mariam@test.com");
        user.setEmailVerified(false);
        user.setVerificationToken("valid-token");
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.verifyEmail("valid-token");

        assertTrue(result.emailVerified());
        assertTrue(user.isEmailVerified());
        assertNull(user.getVerificationToken());
        verify(userEventProducer).sendUserVerifiedEvent(any(UserVerifiedEvent.class));
    }

    @Test
    void verifyEmail_WhenTokenNotFound_ThrowsException() {
        when(userRepository.findByVerificationToken("missing")).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.verifyEmail("missing"));

        assertEquals(400, ex.getStatusCode());
    }

    @Test
    void verifyEmail_WhenAlreadyVerified_ThrowsException() {
        User user = new User();
        user.setId(1L);
        user.setEmailVerified(true);
        user.setVerificationToken("valid-token");
        when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.verifyEmail("valid-token"));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void verifyEmail_WhenTokenExpired_ThrowsException() {
        User user = new User();
        user.setId(1L);
        user.setEmailVerified(false);
        user.setVerificationToken("expired-token");
        user.setVerificationTokenExpiresAt(LocalDateTime.now().minusHours(1));
        when(userRepository.findByVerificationToken("expired-token")).thenReturn(Optional.of(user));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.verifyEmail("expired-token"));

        assertEquals(400, ex.getStatusCode());
    }

    @Test
    void resendVerification_WhenUserExistsAndUnverified_GeneratesNewTokenAndSendsEvent() {
        User user = new User();
        user.setId(1L);
        user.setName("Mariam");
        user.setEmail("mariam@test.com");
        user.setEmailVerified(false);
        when(userRepository.findByEmail("mariam@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.resendVerification("mariam@test.com");

        assertNotNull(user.getVerificationToken());
        verify(userEventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void resendVerification_WhenAlreadyVerified_DoesNothing() {
        User user = new User();
        user.setId(1L);
        user.setEmail("mariam@test.com");
        user.setEmailVerified(true);
        when(userRepository.findByEmail("mariam@test.com")).thenReturn(Optional.of(user));

        userService.resendVerification("mariam@test.com");

        verify(userRepository, never()).save(any(User.class));
        verify(userEventProducer, never()).sendUserRegisteredEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void resendVerification_WhenUserNotFound_DoesNothing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        userService.resendVerification("missing@test.com");

        verify(userRepository, never()).save(any(User.class));
        verify(userEventProducer, never()).sendUserRegisteredEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void forgotPassword_WhenUserExists_GeneratesResetTokenAndSendsEvent() {
        User user = new User();
        user.setId(1L);
        user.setName("Mariam");
        user.setEmail("mariam@test.com");
        when(userRepository.findByEmail("mariam@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.forgotPassword("mariam@test.com");

        assertNotNull(user.getResetToken());
        assertNotNull(user.getResetTokenExpiresAt());
        verify(userEventProducer).sendPasswordResetRequestedEvent(any(PasswordResetRequestedEvent.class));
    }

    @Test
    void forgotPassword_WhenUserNotFound_DoesNothing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        userService.forgotPassword("missing@test.com");

        verify(userRepository, never()).save(any(User.class));
        verify(userEventProducer, never()).sendPasswordResetRequestedEvent(any(PasswordResetRequestedEvent.class));
    }

    @Test
    void resetPassword_WhenTokenValid_UpdatesPasswordAndClearsToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("mariam@test.com");
        user.setPassword("old-encoded");
        user.setResetToken("valid-token");
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newpassword");

        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("new-encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.resetPassword(request);

        assertEquals("new-encoded", user.getPassword());
        assertNull(user.getResetToken());
        assertNull(user.getResetTokenExpiresAt());
    }

    @Test
    void resetPassword_WhenTokenNotFound_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest("missing", "newpassword");
        when(userRepository.findByResetToken("missing")).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.resetPassword(request));

        assertEquals(400, ex.getStatusCode());
    }

    @Test
    void resetPassword_WhenTokenExpired_ThrowsException() {
        User user = new User();
        user.setId(1L);
        user.setEmail("mariam@test.com");
        user.setResetToken("expired-token");
        user.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newpassword");
        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.resetPassword(request));

        assertEquals(400, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAccount_WhenPasswordCorrect_DeletesUserAndPublishesEvent() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setEmail("mariam@test.com");
        user.setPassword("encoded");
        DeleteAccountRequest request = new DeleteAccountRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

        userService.deleteAccount(userId, request);

        verify(userRepository).delete(user);
        verify(userEventProducer).sendUserDeletedEvent(new UserDeletedEvent(userId));
    }

    @Test
    void deleteAccount_WhenPasswordIncorrect_ThrowsAndDoesNotDelete() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setPassword("encoded");
        DeleteAccountRequest request = new DeleteAccountRequest("wrong-password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.deleteAccount(userId, request));

        assertEquals(401, ex.getStatusCode());
        verify(userRepository, never()).delete(any(User.class));
        verify(userEventProducer, never()).sendUserDeletedEvent(any());
    }

    @Test
    void deleteAccount_WhenUserNotFound_ThrowsException() {
        Long userId = 404L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.deleteAccount(userId, new DeleteAccountRequest("password")));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteAccount_WhenEventPublishFails_StillDeletesUser() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setPassword("encoded");
        DeleteAccountRequest request = new DeleteAccountRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        doThrow(new RuntimeException("kafka unavailable"))
                .when(userEventProducer).sendUserDeletedEvent(any(UserDeletedEvent.class));

        userService.deleteAccount(userId, request);

        verify(userRepository).delete(user);
    }
}