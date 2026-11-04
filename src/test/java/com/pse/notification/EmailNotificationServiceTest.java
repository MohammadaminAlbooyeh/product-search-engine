package com.pse.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNotificationServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void sendsMessageWhenEnabled() {
        EmailNotificationService service = new EmailNotificationService(
                mailSender, true, "alerts@example.com");

        service.send("user@example.com", "Price drop", "It got cheaper");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("alerts@example.com");
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getSubject()).isEqualTo("Price drop");
        assertThat(sent.getText()).isEqualTo("It got cheaper");
    }

    @Test
    void doesNotSendWhenDisabled() {
        EmailNotificationService service = new EmailNotificationService(
                mailSender, false, "alerts@example.com");

        service.send("user@example.com", "Price drop", "It got cheaper");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void swallowsMailSenderFailureInsteadOfThrowing() {
        EmailNotificationService service = new EmailNotificationService(
                mailSender, true, "alerts@example.com");
        doThrow(new MailSendException("SMTP unreachable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        service.send("user@example.com", "Price drop", "It got cheaper");
        // reaching here without an exception is the assertion
    }
}
