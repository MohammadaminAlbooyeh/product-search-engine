package com.pse.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional e-mails (currently price-drop alerts).
 *
 * <p>When {@code notifications.email.enabled=false} or no SMTP server is reachable the
 * message is logged instead of sent, so the rest of the pipeline keeps working in
 * local/dev environments without mail infrastructure.
 */
@Service
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailNotificationService(JavaMailSender mailSender,
                                    @Value("${notifications.email.enabled:true}") boolean enabled,
                                    @Value("${notifications.email.from:alerts@product-search-engine.local}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("[email disabled] would send to {} | {} | {}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent e-mail to {} ({})", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send e-mail to {}: {} - falling back to log. Body: {}", to, e.getMessage(), body);
        }
    }
}
