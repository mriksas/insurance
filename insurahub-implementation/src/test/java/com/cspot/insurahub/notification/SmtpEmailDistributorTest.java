package com.cspot.insurahub.notification;

import com.cspot.insurahub.notification.exception.EmailDeliveryException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailDistributorTest {

    private static final String SENDER = "no-reply@example.test";
    private static final String RECIPIENT = "recipient@example.test";
    private static final String SUBJECT = "Test subject";
    private static final String CONTENT = "Test content";

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailDistributor distributor;

    @BeforeEach
    void setUp() {
        distributor = new SmtpEmailDistributor(SENDER, mailSender);
    }

    @Test
    void shouldCreateAndSendEmail() throws Exception {
        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(mailSender.createMimeMessage()).thenReturn(message);

        distributor.sendEmail(RECIPIENT, SUBJECT, CONTENT);

        verify(mailSender).send(message);

        assertThat(message.getFrom())
                .hasSize(1)
                .allSatisfy(from ->
                        assertThat(from.toString()).isEqualTo(SENDER));

        assertThat(message.getRecipients(MimeMessage.RecipientType.TO))
                .hasSize(1)
                .allSatisfy(to ->
                        assertThat(to.toString()).isEqualTo(RECIPIENT));

        assertEquals(SUBJECT, message.getSubject());
        assertEquals(CONTENT, message.getContent());
    }

    @Test
    void shouldWrapMessagingException() throws Exception {
        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(mailSender.createMimeMessage()).thenReturn(message);

        doThrow(new MailSendException("SMTP error"))
                .when(mailSender)
                .send(message);

        assertThatThrownBy(() ->
                distributor.sendEmail(RECIPIENT, SUBJECT, CONTENT)
        )
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessage("Failed to send email")
                .hasCauseInstanceOf(MailException.class);
    }
}
