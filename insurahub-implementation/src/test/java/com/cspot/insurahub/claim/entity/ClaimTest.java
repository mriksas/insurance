package com.cspot.insurahub.claim.entity;

import com.cspot.insurahub.claim.enumeration.ClaimStatus;
import com.cspot.insurahub.enrollment.entity.Enrollment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.cspot.insurahub.enrollment.testdata.EnrollmentTestData.createValidEnrollment;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimTest {

    @Test
    void shouldCreateClaimWithPendingStatus() {
        Enrollment enrollment = createValidEnrollment();

        LocalDate serviceDate = LocalDate.now();
        BigDecimal amount = BigDecimal.valueOf(999.99);

        Claim claim = new Claim(enrollment, serviceDate, amount);

        assertEquals(enrollment, claim.getEnrollment());
        assertEquals(serviceDate, claim.getServiceDate());
        assertEquals(amount, claim.getAmount());
        assertEquals(ClaimStatus.PENDING, claim.getStatus());
        assertEquals(claim, enrollment.getClaims().getFirst());
    }
}
