package com.culitostracker.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Delivers the password-reset link. With SMTP configured (SPRING_MAIL_HOST
 * etc.) it emails the user; without it, the link is written to the server
 * log so an operator can hand it over. The endpoint's response is identical
 * either way, so callers can't probe which mode is active.
 */
@Component
public class ResetLinkSender {

    private static final Logger log = LoggerFactory.getLogger(ResetLinkSender.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public ResetLinkSender(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.from:no-reply@culitostracker.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String email, String preferredLanguage, String link) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("No SMTP configured; password reset link for {}: {}", email, link);
            return;
        }
        boolean en = preferredLanguage != null && preferredLanguage.startsWith("en");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(en ? "Reset your CulitosTracker password"
                : "Restablece tu contraseña de CulitosTracker");
        message.setText(en
                ? "Someone requested a password reset for this account. If it was you, open "
                + "this link within 30 minutes:\n\n" + link
                + "\n\nIf it wasn't you, ignore this email; your password is unchanged."
                : "Alguien pidió restablecer la contraseña de esta cuenta. Si fuiste tú, abre "
                + "este enlace antes de 30 minutos:\n\n" + link
                + "\n\nSi no fuiste tú, ignora este correo; tu contraseña sigue igual.");
        try {
            sender.send(message);
        } catch (RuntimeException e) {
            // The user already got a generic 204: log for the operator only.
            log.error("Failed to send password reset email to {}", email, e);
        }
    }
}
