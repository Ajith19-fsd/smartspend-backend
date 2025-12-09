package com.smartspend.auth.controller;

import com.smartspend.auth.model.User;
import com.smartspend.auth.security.JwtUtils;
import com.smartspend.auth.security.UserDetailsImpl;
import com.smartspend.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    // ================= SIGNUP =================
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            User saved = authService.registerUser(user);

            Map<String, Object> resp = new HashMap<>();
            resp.put("email", saved.getEmail());

            // Return a flag 'verified' so frontend can decide navigation
            if (Boolean.TRUE.equals(saved.getVerified())) {
                resp.put("message", "✅ Registration successful!");
                resp.put("verified", true);
            } else {
                resp.put("message", "✅ Registration successful! OTP sent to your email.");
                resp.put("verified", false);
            }

            // Do not include OTP in response in production
            return ResponseEntity.ok(resp);

        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= VERIFY EMAIL OTP =================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String otp = req.get("otp");

            authService.verifyUserOtp(email, otp);

            return ResponseEntity.ok(Map.of("message", "Email verified successfully! Now you can login."));

        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= RESEND OTP =================
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            authService.resendSignupOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP resent successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginReq) {

        String email = loginReq.get("email");
        String password = loginReq.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email & password are required"));
        }

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        if (!userDetails.isEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please verify your email before login"));
        }

        String jwt = jwtUtils.generateJwtToken(userDetails.getUsername(), userDetails.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", userDetails.getId());
        response.put("email", userDetails.getUsername());
        response.put("roles", userDetails.getAuthorities());

        return ResponseEntity.ok(response);
    }

    // ================= FORGOT PASSWORD =================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> req) {
        try {
            authService.generateResetOtp(req.get("email"));
            return ResponseEntity.ok(Map.of("message", "Reset OTP sent successfully!"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= RESET PASSWORD =================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        try {
            authService.resetPassword(req.get("email"), req.get("otp"), req.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password reset successfully!"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
