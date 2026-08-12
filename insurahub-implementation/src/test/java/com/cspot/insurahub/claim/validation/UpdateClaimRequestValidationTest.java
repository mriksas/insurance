package com.cspot.insurahub.claim.validation;

import com.cspot.insurahub.model.UpdateClaimRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateClaimRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidRequest() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        assertThat(validator.validate(new UpdateClaimRequest()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("serviceDate", "planId", "amount");
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        UpdateClaimRequest request = validRequest().amount(BigDecimal.ZERO);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("amount");
    }

    private UpdateClaimRequest validRequest() {
        return new UpdateClaimRequest()
                .serviceDate(LocalDate.of(2026, 8, 1))
                .planId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100));
    }
}
