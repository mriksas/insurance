package com.cspot.insurahub.enrollment.service;

import com.cspot.insurahub.consumer.entity.Consumer;
import com.cspot.insurahub.consumer.repository.ConsumerRepository;
import com.cspot.insurahub.consumer.service.IdpIdMappingService;
import com.cspot.insurahub.enrollment.entity.Enrollment;
import com.cspot.insurahub.enrollment.entity.EnrollmentStatus;
import com.cspot.insurahub.enrollment.mapper.EnrollmentMapper;
import com.cspot.insurahub.enrollment.repository.EnrollmentRepository;
import com.cspot.insurahub.insurancepackage.enumeration.InsurancePackageStatus;
import com.cspot.insurahub.model.EnrollmentResponse;
import com.cspot.insurahub.model.PostResponse;
import com.cspot.insurahub.plan.entity.InsurancePlan;
import com.cspot.insurahub.plan.repository.InsurancePlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static com.cspot.insurahub.consumer.testdata.ConsumerTestData.createValidConsumerWithId;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidInsurancePlanWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {

    private static UUID CONSUMER_ID = UUID.randomUUID();
    private static UUID PLAN_ID = UUID.randomUUID();

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ConsumerRepository consumerRepository;

    @Mock
    private InsurancePlanRepository insurancePlanRepository;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @Mock
    private IdpIdMappingService idpIdMappingService;

    @Mock
    private EnrollmentValidationService enrollmentValidationService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void shouldEnrollConsumerOnPlan() {
        // GIVEN
        Consumer consumer = createValidConsumerWithId(CONSUMER_ID);
        InsurancePlan plan = createValidInsurancePlanWithId(PLAN_ID, InsurancePackageStatus.INITIALIZED);
        UUID enrollmentId = UUID.randomUUID();

        when(idpIdMappingService.getCurrentAuthenticatedConsumerId()).thenReturn(CONSUMER_ID);
        when(consumerRepository.findByIdOrThrow(CONSUMER_ID)).thenReturn(consumer);
        when(insurancePlanRepository.findByIdOrThrow(PLAN_ID)).thenReturn(plan);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
            return enrollment;
        });

        // WHEN
        PostResponse postResponse = enrollmentService.enrollCurrentAuthenticatedConsumerOnPlan(PLAN_ID);

        // THEN
        assertEquals(enrollmentId, postResponse.getId());
        verify(idpIdMappingService).getCurrentAuthenticatedConsumerId();
        verify(consumerRepository).findByIdOrThrow(CONSUMER_ID);
        verify(insurancePlanRepository).findByIdOrThrow(PLAN_ID);
        verify(enrollmentValidationService).assertConsumerCanEnrollOnPlan(consumer, plan);
        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertEquals(consumer, captor.getValue().getConsumer());
        assertEquals(plan, captor.getValue().getPlan());
    }

    @Test
    void getEnrollmentsShouldReturnMappedEnrollmentsForConsumer() {
        // GIVEN
        when(idpIdMappingService.getCurrentAuthenticatedConsumerId()).thenReturn(CONSUMER_ID);

        // Use mock() because the Enrollment constructor is protected
        Enrollment activeEnrollment = mock(Enrollment.class);
        Enrollment expiredEnrollment = mock(Enrollment.class);

        when(enrollmentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(activeEnrollment, expiredEnrollment));

        EnrollmentResponse response1 = new EnrollmentResponse();
        when(enrollmentMapper.toResponseList(any())).thenReturn(List.of(response1));

        // WHEN
        List<EnrollmentResponse> result = enrollmentService.getEnrollments(EnrollmentStatus.ACTIVE);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(response1);
        verify(enrollmentRepository).findAll(any(Specification.class), any(Sort.class));
        verify(enrollmentMapper).toResponseList(any());
    }

}
