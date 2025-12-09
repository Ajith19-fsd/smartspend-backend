package com.smartspend.auth.service;

import com.smartspend.auth.model.User;
import com.smartspend.auth.model.Role;
import com.smartspend.auth.model.ERole;
import com.smartspend.auth.repository.RoleRepository;
import com.smartspend.auth.repository.UserRepository;
import com.smartspend.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 10; // default

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private Environment env;

    // ===== SIGNUP =====
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // assign ROLE_USER
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default Role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Determine auto-verify from config (application-dev.yaml / application-prod.yaml)
        // Default to false (safest) if property missing
        boolean isDevAutoVerify = Boolean.parseBoolean(env.getProperty("smartspend.otp.auto-verify", "false"));

        if (isDevAutoVerify) {
            // Development mode: skip OTP, mark verified
            user.setVerified(true);
            user.setOtp(null);
            user.setOtpExpiry(null);
            return userRepository.save(user);
        }

        // Production: generate OTP, set expiry, mark not verified
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(getOtpTtlMinutes()));
        user.setVerified(false);

        User savedUser = userRepository.save(user);

        // Send OTP email (EmailService decides dev/prod printing or sending)
        emailService.sendOtpEmail(savedUser.getEmail(), otp);

        return savedUser;
    }

    // ===== Verify signup OTP =====
    public void verifyUserOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getVerified())) {
            return;
        }

        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            throw new RuntimeException("No OTP found. Please request a new OTP.");
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new RuntimeException("OTP expired. Please request a new OTP.");
        }

        if (!otp.equals(user.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
    }

    // ===== Resend OTP =====
    public void resendSignupOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getVerified())) {
            throw new RuntimeException("User already verified");
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(getOtpTtlMinutes()));
        userRepository.save(user);

        emailService.sendOtpEmail(email, otp);
    }

    // ===== FORGOT PASSWORD - generate OTP =====
    public void generateResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String resetOtp = generateOtp();
        user.setResetOtp(resetOtp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(getOtpTtlMinutes()));
        userRepository.save(user);

        emailService.sendResetOtpEmail(email, resetOtp);
    }

    // ===== RESET PASSWORD =====
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetOtp() == null || user.getResetOtpExpiry() == null) {
            throw new RuntimeException("No reset OTP found. Please request a reset OTP.");
        }

        if (LocalDateTime.now().isAfter(user.getResetOtpExpiry())) {
            throw new RuntimeException("Reset OTP expired. Please request a new reset OTP.");
        }

        if (!otp.equals(user.getResetOtp())) {
            throw new RuntimeException("Invalid Reset OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
    }

    // Util
    private String generateOtp() {
        int number = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(number);
    }

    private int getOtpTtlMinutes() {
        try {
            return Integer.parseInt(env.getProperty("smartspend.otp.ttl-minutes", "10"));
        } catch (Exception e) {
            return OTP_TTL_MINUTES;
        }
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
