package com.project.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        if (toEmail == null || toEmail.isEmpty()) {
            return;
        }
        try {
            System.out.println("OTP DELIVERY: Attempting to send email to " + toEmail + " from " + senderEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Verification Code - Care Connect");

            String htmlContent = "<div style='font-family: Inter, Arial, sans-serif; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px; max-width: 500px; margin: auto;'>" +
                    "<h2 style='color: #2563eb; text-align: center;'>Care Connect</h2>" +
                    "<p style='color: #475569; font-size: 16px; text-align: center;'>Please use the following 6-digit code to complete your password reset:</p>" +
                    "<div style='background: #f1f5f9; padding: 15px; border-radius: 8px; text-align: center; font-size: 32px; font-weight: 800; letter-spacing: 10px; color: #1e293b; margin: 20px 0;'>" +
                    otp +
                    "</div>" +
                    "<p style='color: #94a3b8; font-size: 12px; text-align: center;'>If you did not request this, please ignore this email.</p>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("OTP DELIVERY SUCCESS: Email sent to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("OTP DELIVERY FAILED: " + toEmail + " -> " + e.getMessage());
            // We log but don't throw to prevent blocking the user if Mail Server is down
            // They can still see the OTP in console as a fallback in this FYP demo
        }
    }
}
