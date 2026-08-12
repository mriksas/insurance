package com.cspot.insurahub;

import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cspot.insurahub.notification.EmailDistributor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
@Import({BaseIntegrationTest.FixedClockTestConfig.class, BaseIntegrationTest.MockedEmailDistributorTestConfig.class})
public abstract class BaseIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18");

    @TestConfiguration
    static class FixedClockTestConfig {

        @Bean
        @Primary
        public Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-07-09T00:00:00Z"),
                    ZoneId.systemDefault()
            );
        }
    }

    @TestConfiguration
    static class MockedEmailDistributorTestConfig {

        @Bean
        @Primary
        public EmailDistributor mockedEmailDistributor() {
            return Mockito.mock(EmailDistributor.class);
        }
    }
}
