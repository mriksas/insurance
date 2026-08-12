package com.cspot.insurahub.notification;

public interface EmailDistributor {

    void sendEmail(String to, String subject, String content);
}
