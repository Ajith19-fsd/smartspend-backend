package com.smartspend.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private EmailService emailService;

    // Test endpoint to send an email
    @PostMapping("/send")
    public String sendTestEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String message) {
        emailService.sendEmail(to, subject, message);
        return "Email Sent Successfully!";
    }
}
