package com.cspot.insurahub.notification;

import com.cspot.insurahub.notification.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailDistributor implements EmailDistributor {

    private final String senderAddress;
    private final JavaMailSender mailSender;

    @Autowired
    public SmtpEmailDistributor(@Value("${mail.sender}") String senderAddress, JavaMailSender mailSender) {
        this.senderAddress = senderAddress;
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            populateMessageData(to, content, subject, helper);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailDeliveryException("Failed to send email", e);
        }
    }

    private void populateMessageData(String to, String content, String subject, MimeMessageHelper helper)
            throws MessagingException {
        helper.setFrom(senderAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content);
    }
}
