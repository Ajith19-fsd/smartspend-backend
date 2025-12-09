package com.smartspend.email;

import com.smartspend.auth.repository.UserRepository;
import com.smartspend.auth.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Environment env;

    @Autowired
    private UserRepository userRepository; // For userId -> email lookup

    // Get sender email from environment variable
    private String getFromEmail() {
        String from = env.getProperty("SENDER_EMAIL");
        return (from != null && !from.isEmpty()) ? from : "no-reply@smartspend.local";
    }

    private boolean isDev() {
        String active = env.getProperty("spring.profiles.active", "dev");
        return "dev".equalsIgnoreCase(active);
    }

    // Generic send email to specific email address
    public void sendEmail(String toEmail, String subject, String text) {
        if (toEmail == null || toEmail.isEmpty()) return;

        if (isDev()) {
            System.out.println("[DEV - EMAIL] To: " + toEmail + " Subject: " + subject + " Body: " + text);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromEmail());
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // Send email using userId (fetch email from DB)
    public void sendEmail(Long userId, String subject, String text) {
        if (userId == null) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) return;

        sendEmail(user.getEmail(), subject, text);
    }

    // Send OTP after signup
    public void sendOtpEmail(String toEmail, String otp) {
        if (isDev()) {
            System.out.println("[DEV - OTP] To: " + toEmail + " OTP: " + otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromEmail());
        message.setTo(toEmail);
        message.setSubject("SmartSpend Registration OTP");
        message.setText("Your OTP for SmartSpend registration is: " + otp + "\n\nThis OTP expires in 10 minutes.");
        mailSender.send(message);
    }

    // Send OTP for password reset
    public void sendResetOtpEmail(String toEmail, String otp) {
        if (isDev()) {
            System.out.println("[DEV - Reset OTP] To: " + toEmail + " OTP: " + otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromEmail());
        message.setTo(toEmail);
        message.setSubject("SmartSpend Password Reset OTP");
        message.setText("Your OTP to reset password is: " + otp + "\n\nThis OTP expires in 10 minutes.");
        mailSender.send(message);
    }
}
