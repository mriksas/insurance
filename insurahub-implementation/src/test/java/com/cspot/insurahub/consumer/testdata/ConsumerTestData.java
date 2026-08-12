package com.cspot.insurahub.consumer.testdata;

import com.cspot.insurahub.consumer.entity.Consumer;
import com.cspot.insurahub.model.ConsumerResponse;
import com.cspot.insurahub.model.PostConsumerRequest;
import com.cspot.insurahub.model.PutConsumerRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

public final class ConsumerTestData {

    private ConsumerTestData() {
    }

    public static Consumer createValidConsumer() {
        return createValidConsumer("idpId");
    }

    public static Consumer createValidConsumer(String idpId) {
        Consumer consumer = new Consumer();
        consumer.setEmail("email@email.org");
        consumer.setIdpId(idpId);
        consumer.setFirstName("First Name");
        consumer.setLastName("Last Name");
        consumer.setPersonalId("12345678910");
        consumer.setDateOfBirth(LocalDate.of(2026, 7, 7));
        consumer.setAddress("Address");
        consumer.setCity("City");
        return consumer;
    }

    public static Consumer createValidConsumerWithId(UUID id) {
        Consumer consumer = createValidConsumer();
        ReflectionTestUtils.setField(consumer, "id", id);
        return consumer;
    }

    public static Consumer createUniqueValidConsumer() {
        Consumer consumer = new Consumer();
        String uniqueValue = UUID.randomUUID().toString();
        consumer.setIdpId("auth0|" + uniqueValue);
        consumer.setEmail(uniqueValue + "@email.org");
        consumer.setFirstName("First Name");
        consumer.setLastName("Last Name");
        consumer.setPersonalId(uniqueValue.substring(0, 11));
        consumer.setDateOfBirth(LocalDate.of(2026, 7, 7));
        consumer.setAddress("Address");
        consumer.setCity("City");
        return consumer;
    }

    public static PostConsumerRequest createValidPostConsumerRequest() {
        return new PostConsumerRequest()
                .email("email@email.org")
                .password("SecurePassword123")
                .firstName("First Name")
                .lastName("Last Name")
                .personalId("12345678910")
                .dateOfBirth(LocalDate.of(2026, 7, 7))
                .address("Address")
                .city("City");
    }

    public static ConsumerResponse createValidConsumerResponse() {
        return new ConsumerResponse()
                .id(UUID.randomUUID())
                .firstName("First Name")
                .lastName("Last Name")
                .fullName("First Name Last Name")
                .personalId("12345678910")
                .dateOfBirth(LocalDate.of(2026, 7, 7));
    }

    public static PutConsumerRequest createValidPutConsumerRequest() {
        return new PutConsumerRequest()
                .firstName("First Name")
                .lastName("Last Name")
                .personalId("12345678910")
                .dateOfBirth(LocalDate.of(2026, 7, 7))
                .address("Address")
                .city("City");
    }

    public static PutConsumerRequest createValidUpdatedPutConsumerRequest() {
        return createValidPutConsumerRequest()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .city("Updated City");
    }
}
