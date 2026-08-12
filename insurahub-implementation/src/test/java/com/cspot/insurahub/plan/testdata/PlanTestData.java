package com.cspot.insurahub.plan.testdata;

import com.cspot.insurahub.insurancepackage.entity.InsurancePackage;
import com.cspot.insurahub.insurancepackage.enumeration.InsurancePackageStatus;
import com.cspot.insurahub.model.PlanRequest;
import com.cspot.insurahub.model.PlanResponse;
import com.cspot.insurahub.model.PlanType;
import com.cspot.insurahub.plan.entity.InsurancePlan;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static com.cspot.insurahub.insurancepackage.testdata.InsurancePackageTestData.createValidInsurancePackage;

public final class PlanTestData {

    private PlanTestData() {
    }

    public static PlanRequest createValidPlanRequest() {
        return new PlanRequest(
                "Standard Health",
                PlanType.HEALTH_INSURANCE,
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(500)
        );
    }

    public static PlanRequest createValidDentalPlanRequest() {
        return createValidPlanRequest()
                .name("Dental Basic")
                .type(PlanType.DENTAL_INSURANCE)
                .contribution(BigDecimal.valueOf(100))
                .election(BigDecimal.valueOf(300));
    }

    public static PlanRequest createValidUpdatedDentalPlanRequest() {
        return createValidPlanRequest()
                .name("Updated Dental")
                .type(PlanType.DENTAL_INSURANCE)
                .contribution(BigDecimal.valueOf(300))
                .election(BigDecimal.valueOf(600));
    }

    public static String createValidPlanRequestBody(
            String name,
            String type,
            int contribution,
            int election
    ) {
        return """
                {
                  "name": "%s",
                  "type": "%s",
                  "contribution": %d,
                  "election": %d
                }
                """.formatted(name, type, contribution, election);
    }

    public static InsurancePlan createValidInsurancePlan(InsurancePackage insurancePackage) {
        return createValidInsurancePlan(insurancePackage, "Plan");
    }

    public static InsurancePlan createValidInsurancePlan(InsurancePackage insurancePackage, String name) {
        return new InsurancePlan(
                insurancePackage,
                name,
                PlanType.HEALTH_INSURANCE,
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(500)
        );
    }

    public static InsurancePlan createValidStandardHealthInsurancePlan(InsurancePackage insurancePackage) {
        return createValidInsurancePlan(insurancePackage, "Standard Health");
    }

    public static InsurancePlan createValidDentalBasicInsurancePlan(InsurancePackage insurancePackage) {
        return new InsurancePlan(
                insurancePackage,
                "Dental Basic",
                PlanType.DENTAL_INSURANCE,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(300));
    }

    public static InsurancePlan createValidInsurancePlanWithId(UUID id) {
        InsurancePlan plan = createValidInsurancePlan(createValidInsurancePackage());
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }

    public static InsurancePlan createValidInsurancePlanWithId(UUID id, InsurancePackageStatus packageStatus) {
        InsurancePackage insurancePackage = createValidInsurancePackage();
        insurancePackage.setStatus(packageStatus);
        InsurancePlan plan = createValidInsurancePlan(insurancePackage);
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }

    public static PlanResponse createValidPlanResponse() {
        return createValidPlanResponse(
                UUID.randomUUID(),
                "Standard Health",
                PlanType.HEALTH_INSURANCE,
                250,
                500
        );
    }

    public static PlanResponse createValidPlanResponse(
            UUID id,
            String name,
            PlanType type,
            int contribution,
            int election
    ) {
        return new PlanResponse(
                id,
                name,
                type,
                contribution,
                election
        );
    }
}
