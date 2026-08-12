package com.cspot.insurahub.plan.service;

import com.cspot.insurahub.auth.service.AuthenticationMetadataQueryService;
import com.cspot.insurahub.common.exception.ResourceNotFoundException;
import com.cspot.insurahub.insurancepackage.entity.InsurancePackage;
import com.cspot.insurahub.insurancepackage.enumeration.InsurancePackageStatus;
import com.cspot.insurahub.insurancepackage.exception.PackageNotFoundException;
import com.cspot.insurahub.insurancepackage.exception.PackageUpdateNotAllowedException;
import com.cspot.insurahub.insurancepackage.repository.InsurancePackageRepository;
import com.cspot.insurahub.insurancepackage.validation.PackageValidator;
import com.cspot.insurahub.model.PlanRequest;
import com.cspot.insurahub.model.PlanResponse;
import com.cspot.insurahub.plan.entity.InsurancePlan;
import com.cspot.insurahub.plan.mapper.PlanMapper;
import com.cspot.insurahub.plan.repository.InsurancePlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static com.cspot.insurahub.insurancepackage.testdata.InsurancePackageTestData.createValidInsurancePackage;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidDentalPlanRequest;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidInsurancePlan;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidPlanResponse;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidPlanRequest;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidUpdatedDentalPlanRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private InsurancePackageRepository packageRepository;

    @Mock
    private InsurancePlanRepository planRepository;

    @Mock
    private PlanMapper planMapper;

    @Mock
    private PackageValidator packageValidator;

    @Mock
    private AuthenticationMetadataQueryService authenticationMetadataQueryService;

    @InjectMocks
    private PlanService planService;

    @Test
    void shouldAddPlanToPackage() {
        UUID packageId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        InsurancePackage insurancePackage = createValidInsurancePackage();
        PlanRequest request = createValidPlanRequest();
        InsurancePlan plan = createValidInsurancePlan(insurancePackage);
        ReflectionTestUtils.setField(plan, "id", planId);

        when(packageRepository.findByIdOrThrow(packageId))
                .thenReturn(insurancePackage);
        when(planMapper.toEntity(insurancePackage, request))
                .thenReturn(plan);
        when(planRepository.save(plan))
                .thenReturn(plan);

        assertThat(planService.addPlan(packageId, request).getId())
                .isEqualTo(planId);

        verify(packageRepository).findByIdOrThrow(packageId);
        verify(packageValidator).validateReadyForUpdate(insurancePackage);
        verify(planMapper).toEntity(insurancePackage, request);
        verify(planRepository).save(plan);
    }

    @Test
    void shouldRejectAddingPlanToInitializedPackage() {
        UUID packageId = UUID.randomUUID();
        InsurancePackage insurancePackage = createValidInsurancePackage();
        insurancePackage.setStatus(InsurancePackageStatus.INITIALIZED);
        PlanRequest request = createValidDentalPlanRequest();
        when(packageRepository.findByIdOrThrow(packageId))
                .thenReturn(insurancePackage);
        doThrow(new PackageUpdateNotAllowedException(
                "Package updates are only allowed when the status is NOT_STARTED"
        )).when(packageValidator).validateReadyForUpdate(insurancePackage);

        assertThatThrownBy(() -> planService.addPlan(packageId, request))
                .isInstanceOf(PackageUpdateNotAllowedException.class);

        verify(packageValidator).validateReadyForUpdate(insurancePackage);
        verify(planMapper, never()).toEntity(
                any(InsurancePackage.class),
                any(PlanRequest.class)
        );
        verify(planRepository, never())
                .save(any(InsurancePlan.class));
    }

    @Test
    void shouldThrowOnAddPlanWhenPackageDoesNotExist() {
        UUID packageId = UUID.randomUUID();
        PlanRequest request = createValidPlanRequest();

        when(packageRepository.findByIdOrThrow(packageId))
                .thenThrow(new PackageNotFoundException(packageId));

        assertThatThrownBy(() -> planService.addPlan(packageId, request))
                .isInstanceOf(PackageNotFoundException.class);

        verify(packageRepository).findByIdOrThrow(packageId);
        verify(packageValidator, never())
                .validateReadyForUpdate(any(InsurancePackage.class));
        verify(planRepository, never()).save(any(InsurancePlan.class));
    }

    @Test
    void shouldReturnPlansOfPackage() {
        UUID packageId = UUID.randomUUID();
        InsurancePackage insurancePackage = createValidInsurancePackage();
        InsurancePlan plan = createValidInsurancePlan(insurancePackage);
        insurancePackage.getPlans().add(plan);
        Pageable pageable = PageRequest.of(0, 10);
        Page<PlanResponse> expectedResponses = new PageImpl<>(
                List.of(createValidPlanResponse()),
                pageable,
                1
        );

        when(packageRepository.existsById(packageId))
                .thenReturn(true);
        when(planRepository.findByInsurancePackageId(packageId, pageable))
                .thenReturn(new PageImpl<>(List.of(plan), pageable, 1));
        when(planMapper.toPlanResponse(plan))
                .thenReturn(expectedResponses.getContent().get(0));

        assertThat(planService.getPackagePlans(packageId, pageable))
                .isEqualTo(expectedResponses);

        verify(packageRepository).existsById(packageId);
        verify(planMapper).toPlanResponse(plan);
    }

    @Test
    void shouldThrowOnGetPackagePlansWhenPackageDoesNotExist() {
        UUID packageId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(packageRepository.existsById(packageId))
                .thenReturn(false);

        assertThatThrownBy(() -> planService.getPackagePlans(packageId, pageable))
                .isInstanceOf(PackageNotFoundException.class);

        verify(planRepository, never())
                .findByInsurancePackageId(any(UUID.class), any(Pageable.class));
    }

    @Test
    void shouldReturnOnlyPlansFromInitializedPackages() {
        InsurancePackage initializedPackage = createValidInsurancePackage();
        initializedPackage.setStatus(InsurancePackageStatus.INITIALIZED);
        InsurancePlan plan = createValidInsurancePlan(initializedPackage);
        PlanResponse expectedResponse = createValidPlanResponse();

        when(planRepository.findByInsurancePackageStatus(InsurancePackageStatus.INITIALIZED))
                .thenReturn(List.of(plan));
        when(planMapper.toPlanResponse(plan))
                .thenReturn(expectedResponse);

        List<PlanResponse> result = planService.getAvailablePlans();

        assertThat(result).containsExactly(expectedResponse);
        verify(planRepository).findByInsurancePackageStatus(InsurancePackageStatus.INITIALIZED);
    }

    @Test
    void shouldReturnEmptyListWhenNoInitializedPackagesHavePlans() {
        when(planRepository.findByInsurancePackageStatus(InsurancePackageStatus.INITIALIZED))
                .thenReturn(List.of());

        List<PlanResponse> result = planService.getAvailablePlans();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdatePlan() {
        UUID planId = UUID.randomUUID();

        InsurancePackage insurancePackage = createValidInsurancePackage();
        InsurancePlan plan = createValidInsurancePlan(insurancePackage);

        PlanRequest request = createValidUpdatedDentalPlanRequest();

        when(planRepository.findByIdOrThrow(planId))
                .thenReturn(plan);

        planService.updatePlan(planId, request);

        verify(planRepository).findByIdOrThrow(planId);
        verify(packageValidator).validateReadyForUpdate(insurancePackage);
        verify(planMapper).updateFromUpdateRequest(plan, request);
    }

    @Test
    void shouldRejectUpdatePlanWhenPackageIsNotReadyForUpdate() {
        UUID planId = UUID.randomUUID();

        InsurancePackage insurancePackage = createValidInsurancePackage();
        InsurancePlan plan = createValidInsurancePlan(insurancePackage);

        PlanRequest request = createValidUpdatedDentalPlanRequest();

        when(planRepository.findByIdOrThrow(planId))
                .thenReturn(plan);

        doThrow(new PackageUpdateNotAllowedException(
                "Package updates are only allowed when the status is NOT_STARTED"
        )).when(packageValidator).validateReadyForUpdate(insurancePackage);

        assertThatThrownBy(() -> planService.updatePlan(planId, request))
                .isInstanceOf(PackageUpdateNotAllowedException.class);

        verify(planMapper, never())
                .updateFromUpdateRequest(any(), any());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistingPlan() {
        UUID planId = UUID.randomUUID();

        PlanRequest request = createValidUpdatedDentalPlanRequest();

        when(planRepository.findByIdOrThrow(planId))
                .thenThrow(new ResourceNotFoundException(InsurancePlan.class, planId));

        assertThatThrownBy(() -> planService.updatePlan(planId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(planRepository).findByIdOrThrow(planId);
        verify(packageValidator, never())
                .validateReadyForUpdate(any());

        verify(planMapper, never())
                .updateFromUpdateRequest(any(), any());
    }

    @Test
    void shouldDeletePlan() {
        UUID planId = UUID.randomUUID();

        InsurancePackage insurancePackage = createValidInsurancePackage();
        InsurancePlan plan = createValidInsurancePlan(insurancePackage);

        when(planRepository.findByIdOrThrow(planId))
                .thenReturn(plan);
        when(authenticationMetadataQueryService.getRequiredAuthenticatedPrincipalName())
                .thenReturn("admin-user");

        planService.deletePlan(planId);

        assertThat(plan.isDeleted()).isTrue();
        assertThat(plan.getDeletedBy()).isEqualTo("admin-user");
        verify(packageValidator).validateReadyForUpdate(insurancePackage);
        verify(authenticationMetadataQueryService).getRequiredAuthenticatedPrincipalName();
    }

    @Test
    void shouldRejectDeletingPlanWhenPackageIsInitialized() {
        UUID planId = UUID.randomUUID();

        InsurancePackage insurancePackage = createValidInsurancePackage();
        insurancePackage.setStatus(InsurancePackageStatus.INITIALIZED);
        InsurancePlan plan = createValidInsurancePlan(insurancePackage);

        when(planRepository.findByIdOrThrow(planId))
                .thenReturn(plan);

        doThrow(new PackageUpdateNotAllowedException(
                "Package updates are only allowed when the status is NOT_STARTED"
        )).when(packageValidator).validateReadyForUpdate(insurancePackage);

        assertThatThrownBy(() -> planService.deletePlan(planId))
                .isInstanceOf(PackageUpdateNotAllowedException.class);

        assertThat(plan.isDeleted()).isFalse();
        verify(authenticationMetadataQueryService, never())
                .getRequiredAuthenticatedPrincipalName();
    }

    @Test
    void shouldThrowOnDeletePlanWhenPlanDoesNotExist() {
        UUID planId = UUID.randomUUID();

        when(planRepository.findByIdOrThrow(planId))
                .thenThrow(new ResourceNotFoundException(InsurancePlan.class, planId));

        assertThatThrownBy(() -> planService.deletePlan(planId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(packageValidator, never())
                .validateReadyForUpdate(any());

        verify(authenticationMetadataQueryService, never())
                .getRequiredAuthenticatedPrincipalName();
    }

}
