package am.techshop.user.service;

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
import am.techshop.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_WhenEmailNotTaken_RegistersUser() {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", null);
        User savedUser = User.builder()
                .id(1L)
                .name("Mariam")
                .email("mariam@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .build();
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
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
        User savedUser = User.builder().id(1L).name("Mariam").email("mariam@test.com").role(UserRole.CUSTOMER).build();
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.CUSTOMER, captor.getValue().getRole());
    }

    @Test
    void register_WhenRoleProvided_UsesRequestedRole() {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", UserRole.ADMIN);
        User savedUser = User.builder().id(1L).name("Mariam").email("mariam@test.com").role(UserRole.ADMIN).build();
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.ADMIN, LocalDateTime.now(), true);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.ADMIN, captor.getValue().getRole());
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
        User user = User.builder()
                .id(1L)
                .email("mariam@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .build();
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
        User user = User.builder()
                .id(1L)
                .email("mariam@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.login(request));

        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void getUserById_WhenExists_ReturnsUser() {
        Long id = 1L;
        User user = User.builder().id(id).name("Mariam").build();
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
        User user = User.builder().id(id).name("Mariam").role(UserRole.CUSTOMER).build();
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
        ReflectionTestUtils.setField(userService, "requireEmailVerification", true);
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        User user = User.builder()
                .id(1L)
                .email("mariam@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.login(request));

        assertEquals(403, ex.getStatusCode());
    }

    @Test
    void login_WhenUnverifiedButVerificationNotRequired_Succeeds() {
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        User user = User.builder()
                .id(1L)
                .email("mariam@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .emailVerified(false)
                .build();
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
        User user = User.builder()
                .id(1L)
                .name("Mariam")
                .email("mariam@test.com")
                .emailVerified(false)
                .verificationToken("valid-token")
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
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
        User user = User.builder()
                .id(1L)
                .emailVerified(true)
                .verificationToken("valid-token")
                .build();
        when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.verifyEmail("valid-token"));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void verifyEmail_WhenTokenExpired_ThrowsException() {
        User user = User.builder()
                .id(1L)
                .emailVerified(false)
                .verificationToken("expired-token")
                .verificationTokenExpiresAt(LocalDateTime.now().minusHours(1))
                .build();
        when(userRepository.findByVerificationToken("expired-token")).thenReturn(Optional.of(user));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.verifyEmail("expired-token"));

        assertEquals(400, ex.getStatusCode());
    }

    @Test
    void resendVerification_WhenUserExistsAndUnverified_GeneratesNewTokenAndSendsEvent() {
        User user = User.builder()
                .id(1L)
                .name("Mariam")
                .email("mariam@test.com")
                .emailVerified(false)
                .build();
        when(userRepository.findByEmail("mariam@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.resendVerification("mariam@test.com");

        assertNotNull(user.getVerificationToken());
        verify(userEventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void resendVerification_WhenAlreadyVerified_DoesNothing() {
        User user = User.builder()
                .id(1L)
                .email("mariam@test.com")
                .emailVerified(true)
                .build();
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
        User user = User.builder()
                .id(1L)
                .name("Mariam")
                .email("mariam@test.com")
                .build();
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
        User user = User.builder()
                .id(1L)
                .email("mariam@test.com")
                .password("old-encoded")
                .resetToken("valid-token")
                .resetTokenExpiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
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
        User user = User.builder()
                .id(1L)
                .email("mariam@test.com")
                .resetToken("expired-token")
                .resetTokenExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newpassword");
        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> userService.resetPassword(request));

        assertEquals(400, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }
}