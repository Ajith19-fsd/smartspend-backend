package com.smartspend.auth.service;

import com.smartspend.auth.model.ERole;
import com.smartspend.auth.model.Role;
import com.smartspend.auth.model.User;
import com.smartspend.auth.repository.RoleRepository;
import com.smartspend.auth.repository.UserRepository;
import com.smartspend.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private Environment env;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        user = new User("test@gmail.com", "password123", "Test User");
        user.setId(1L);

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(ERole.ROLE_USER);
    }

    // ✅ Register user - success (DEV auto verify)
    @Test
    void registerUser_devAutoVerify_shouldRegisterSuccessfully() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(env.getProperty("smartspend.otp.auto-verify", "false")).thenReturn("true");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = authService.registerUser(user);

        assertNotNull(saved);
        assertTrue(saved.getVerified());
        verify(emailService, never()).sendOtpEmail(any(), any());
    }

    // ✅ Register user - email already exists
    @Test
    void registerUser_emailExists_shouldThrowException() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.registerUser(user));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // ✅ Register user - PROD mode (OTP sent)
    @Test
    void registerUser_prodMode_shouldGenerateOtpAndSendEmail() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(env.getProperty("smartspend.otp.auto-verify", "false")).thenReturn("false");
        when(env.getProperty("smartspend.otp.ttl-minutes", "10")).thenReturn("10");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = authService.registerUser(user);

        assertFalse(saved.getVerified());
        assertNotNull(saved.getOtp());
        assertNotNull(saved.getOtpExpiry());
        verify(emailService, times(1)).sendOtpEmail(eq(user.getEmail()), any());
    }

    // ✅ Verify OTP - success
    @Test
    void verifyUserOtp_validOtp_shouldVerifyUser() {
        user.setOtp("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setVerified(false);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.verifyUserOtp(user.getEmail(), "123456");

        assertTrue(user.getVerified());
        assertNull(user.getOtp());
        assertNull(user.getOtpExpiry());
    }

    // ✅ Verify OTP - invalid OTP
    @Test
    void verifyUserOtp_invalidOtp_shouldThrowException() {
        user.setOtp("111111");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.verifyUserOtp(user.getEmail(), "222222"));

        assertEquals("Invalid OTP", ex.getMessage());
    }

    // ✅ Resend OTP
    @Test
    void resendSignupOtp_shouldGenerateNewOtp() {
        user.setVerified(false);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(env.getProperty("smartspend.otp.ttl-minutes", "10")).thenReturn("10");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.resendSignupOtp(user.getEmail());

        assertNotNull(user.getOtp());
        assertNotNull(user.getOtpExpiry());
        verify(emailService, times(1)).sendOtpEmail(eq(user.getEmail()), any());
    }

    // ✅ Generate reset OTP
    @Test
    void generateResetOtp_shouldSetResetOtp() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(env.getProperty("smartspend.otp.ttl-minutes", "10")).thenReturn("10");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.generateResetOtp(user.getEmail());

        assertNotNull(user.getResetOtp());
        assertNotNull(user.getResetOtpExpiry());
        verify(emailService, times(1)).sendResetOtpEmail(eq(user.getEmail()), any());
    }

    // ✅ Reset password success
    @Test
    void resetPassword_validOtp_shouldResetPassword() {
        user.setResetOtp("999999");
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("newEncodedPass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.resetPassword(user.getEmail(), "999999", "newPassword");

        assertNull(user.getResetOtp());
        assertNull(user.getResetOtpExpiry());
        verify(passwordEncoder, times(1)).encode("newPassword");
    }
}
