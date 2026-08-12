package com.cspot.insurahub.claim.testdata;

import com.cspot.insurahub.claim.entity.Claim;
import com.cspot.insurahub.consumer.entity.Consumer;
import com.cspot.insurahub.enrollment.entity.Enrollment;
import com.cspot.insurahub.insurancepackage.entity.InsurancePackage;
import com.cspot.insurahub.model.ClaimResponse;
import com.cspot.insurahub.model.PostClaimRequest;
import com.cspot.insurahub.plan.entity.InsurancePlan;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.cspot.insurahub.consumer.testdata.ConsumerTestData.createValidConsumerWithId;
import static com.cspot.insurahub.enrollment.testdata.EnrollmentTestData.createValidEnrollment;
import static com.cspot.insurahub.enrollment.testdata.EnrollmentTestData.createValidEnrollmentWithId;
import static com.cspot.insurahub.insurancepackage.testdata.InsurancePackageTestData.createValidInsurancePackage;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidStandardHealthInsurancePlan;

public final class ClaimTestData {

    private ClaimTestData() {
    }

    public static PostClaimRequest createValidPostClaimRequest(UUID enrollmentId) {
        return new PostClaimRequest()
                .enrollmentId(enrollmentId)
                .serviceDate(LocalDate.of(2026, 7, 10))
                .amount(BigDecimal.valueOf(123.45));
    }

    public static Claim createValidClaim() {
        return new Claim(
                createValidEnrollmentWithId(UUID.randomUUID()),
                LocalDate.of(2026, 7, 10),
                BigDecimal.valueOf(123.45)
        );
    }

    public static Claim createValidClaimForListResponse() {
        Consumer employee = createValidConsumerWithId(UUID.randomUUID());
        employee.setFirstName("John");
        employee.setLastName("Doe");

        InsurancePackage insurancePackage = createValidInsurancePackage();
        insurancePackage.setStartDate(LocalDate.of(2026, 1, 1));
        insurancePackage.setEndDate(LocalDate.of(2026, 12, 31));

        InsurancePlan plan = createValidStandardHealthInsurancePlan(insurancePackage);
        ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());

        Enrollment enrollment = createValidEnrollment(employee, plan);
        ReflectionTestUtils.setField(enrollment, "id", UUID.randomUUID());

        Claim claim = new Claim(enrollment, LocalDate.of(2026, 7, 15), new BigDecimal("285.50"));
        claim.setClaimNumber("LT20260715001");
        ReflectionTestUtils.setField(claim, "id", UUID.randomUUID());
        return claim;
    }

    public static ClaimResponse createValidClaimResponse() {
        return new ClaimResponse()
                .id(UUID.randomUUID())
                .claimNumber("LT20260715001")
                .consumerFullName("John Doe")
                .serviceDate(LocalDate.of(2026, 7, 15))
                .planName("Standard Health")
                .amount(new BigDecimal("285.50"))
                .status(com.cspot.insurahub.model.ClaimStatus.PENDING);
    }
}
