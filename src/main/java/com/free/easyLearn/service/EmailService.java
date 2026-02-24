package com.free.easyLearn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String name, String token) {
        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@lingua-hub.com");
        message.setTo(toEmail);
        message.setSubject("LinguaHub - Vérification de votre adresse email");
        message.setText(
                "Bonjour " + name + ",\n\n" +
                "Merci de vous être inscrit sur LinguaHub en tant que professeur.\n\n" +
                "Pour activer votre compte, veuillez cliquer sur le lien suivant :\n" +
                verificationLink + "\n\n" +
                "Ce lien est valable pendant 24 heures.\n\n" +
                "Si vous n'avez pas créé de compte, vous pouvez ignorer cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe LinguaHub"
        );

        try {
            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Impossible d'envoyer l'email de vérification");
        }
    }
}
