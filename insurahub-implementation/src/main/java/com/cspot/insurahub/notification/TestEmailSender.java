package com.cspot.insurahub.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * This class is temporary and intended only for demonstrating that emails are sent.
 */
@Component
@RequiredArgsConstructor
public class TestEmailSender implements CommandLineRunner {

    private final EmailDistributor emailDistributor;

    @Override
    public void run(String... args) throws Exception {
        emailDistributor.sendEmail("test.recipient@email.org", "Title", "Hello");
    }
}
