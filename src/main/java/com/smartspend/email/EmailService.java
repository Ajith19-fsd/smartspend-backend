package com.smartspend.email;

import com.smartspend.auth.model.User;
import com.smartspend.auth.repository.UserRepository;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Autowired
    private Environment env;

    @Autowired
    private UserRepository userRepository;

    // Check if current profile is dev
    private boolean isDev() {
        String active = env.getProperty("spring.profiles.active", "dev");
        return "dev".equalsIgnoreCase(active);
    }

    // Get sender email (Safe fallback logic)
    private String getFromEmail() {
        // First try official location
        String from = env.getProperty("smartspend.email.sender");
        // Then fallback to raw SENDER_EMAIL var
        if (from == null || from.isEmpty()) {
            from = env.getProperty("SENDER_EMAIL");
        }
        // Final fallback
        return (from != null && !from.isEmpty()) ? from : "no-reply@smartspend.local";
    }

    // Get SendGrid API key
    private String getSendGridApiKey() {
        String apiKey = env.getProperty("SENDGRID_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("SENDGRID_API_KEY is not set in environment variables");
        }
        return apiKey;
    }

    // Generic method to send email
    public void sendEmail(String toEmail, String subject, String text) {
        if (toEmail == null || toEmail.isEmpty()) return;

        if (isDev()) {
            System.out.println("[DEV - EMAIL] To: " + toEmail + " Subject: " + subject + " Body: " + text);
            return;
        }

        Email from = new Email(getFromEmail());
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", text);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(getSendGridApiKey());
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            System.out.println("[SENDGRID] Status Code: " + response.getStatusCode());
            System.out.println("[SENDGRID] Body: " + response.getBody());
        } catch (IOException ex) {
            System.err.println("[SENDGRID] Error sending email: " + ex.getMessage());
        }
    }

    // Send email using userId
    public void sendEmail(Long userId, String subject, String text) {
        if (userId == null) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) return;

        sendEmail(user.getEmail(), subject, text);
    }

    // Send OTP after signup
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "SmartSpend Registration OTP";
        String body = "Your OTP for SmartSpend registration is: " + otp + "\n\nThis OTP expires in 10 minutes.";
        sendEmail(toEmail, subject, body);
    }

    // Send OTP for password reset
    public void sendResetOtpEmail(String toEmail, String otp) {
        String subject = "SmartSpend Password Reset OTP";
        String body = "Your OTP to reset password is: " + otp + "\n\nThis OTP expires in 10 minutes.";
        sendEmail(toEmail, subject, body);
    }
}
