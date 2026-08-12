package com.cspot.insurahub.enrollment.testdata;

import com.cspot.insurahub.consumer.entity.Consumer;
import com.cspot.insurahub.enrollment.entity.Enrollment;
import com.cspot.insurahub.plan.entity.InsurancePlan;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static com.cspot.insurahub.consumer.testdata.ConsumerTestData.createValidConsumerWithId;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidInsurancePlanWithId;
import static com.cspot.insurahub.consumer.testdata.ConsumerTestData.createValidConsumer;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidInsurancePlan;
import static com.cspot.insurahub.insurancepackage.testdata.InsurancePackageTestData.createValidInsurancePackage;

public final class EnrollmentTestData {

    private EnrollmentTestData() {
    }

    public static Enrollment createValidEnrollment(Consumer consumer, InsurancePlan plan) {
        return new Enrollment(consumer, plan);
    }

    public static Enrollment createValidEnrollment() {
        return createValidEnrollment(
                createValidConsumer(),
                createValidInsurancePlan(createValidInsurancePackage())
        );
    }

    public static Enrollment createValidEnrollmentWithId(UUID id) {
        Enrollment enrollment = createValidEnrollment(
                createValidConsumerWithId(UUID.randomUUID()),
                createValidInsurancePlanWithId(UUID.randomUUID())
        );
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }
}
