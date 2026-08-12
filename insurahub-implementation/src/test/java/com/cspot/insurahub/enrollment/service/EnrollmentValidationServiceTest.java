package com.cspot.insurahub.enrollment.service;

import com.cspot.insurahub.consumer.entity.Consumer;
import com.cspot.insurahub.enrollment.exception.EnrollmentDeniedException;
import com.cspot.insurahub.enrollment.repository.EnrollmentRepository;
import com.cspot.insurahub.insurancepackage.enumeration.InsurancePackageStatus;
import com.cspot.insurahub.plan.entity.InsurancePlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.cspot.insurahub.consumer.testdata.ConsumerTestData.createValidConsumerWithId;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidInsurancePlanWithId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentValidationServiceTest {

    private static UUID CONSUMER_ID = UUID.randomUUID();
    private static UUID PLAN_ID = UUID.randomUUID();

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentValidationService service;

    @Test
    void shouldAllowEnrollmentWhenPlanIsInitializedAndConsumerNotAlreadyEnrolled() {
        Consumer consumer = createValidConsumerWithId(CONSUMER_ID);
        InsurancePlan plan = createValidInsurancePlanWithId(PLAN_ID, InsurancePackageStatus.INITIALIZED);

        when(enrollmentRepository.existsByConsumerIdAndPlanId(CONSUMER_ID, PLAN_ID))
                .thenReturn(false);

        assertDoesNotThrow(() ->
                service.assertConsumerCanEnrollOnPlan(consumer, plan));

        verify(enrollmentRepository)
                .existsByConsumerIdAndPlanId(CONSUMER_ID, PLAN_ID);
    }

    @Test
    void shouldThrowWhenPlanPackageIsNotInitialized() {
        Consumer consumer = createValidConsumerWithId(CONSUMER_ID);
        InsurancePlan plan = createValidInsurancePlanWithId(PLAN_ID, InsurancePackageStatus.NOT_STARTED);

        EnrollmentDeniedException exception = assertThrows(
                EnrollmentDeniedException.class,
                () -> service.assertConsumerCanEnrollOnPlan(consumer, plan)
        );

        assertEquals(
                "Only packages with status INITIALIZED can be enrolled on",
                exception.getMessage()
        );

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void shouldThrowWhenConsumerAlreadyEnrolled() {
        Consumer consumer = createValidConsumerWithId(CONSUMER_ID);
        InsurancePlan plan = createValidInsurancePlanWithId(PLAN_ID, InsurancePackageStatus.INITIALIZED);

        when(enrollmentRepository.existsByConsumerIdAndPlanId(CONSUMER_ID, PLAN_ID))
                .thenReturn(true);

        EnrollmentDeniedException exception = assertThrows(
                EnrollmentDeniedException.class,
                () -> service.assertConsumerCanEnrollOnPlan(consumer, plan)
        );

        assertEquals(
                "You are already enrolled on this plan",
                exception.getMessage()
        );

        verify(enrollmentRepository)
                .existsByConsumerIdAndPlanId(CONSUMER_ID, PLAN_ID);
    }

}
