package com.cspot.insurahub.insurancepackage.testdata;

import com.cspot.insurahub.insurancepackage.entity.InsurancePackage;
import com.cspot.insurahub.insurancepackage.enumeration.InsurancePackageStatus;
import com.cspot.insurahub.model.PackageRequest;
import com.cspot.insurahub.model.PackageResponse;
import com.cspot.insurahub.payroll.Payroll;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

public final class InsurancePackageTestData {

    private InsurancePackageTestData() {
    }

    public static InsurancePackage createValidInsurancePackage() {
        return new InsurancePackage(
                "Premium Health Package",
                Payroll.MONTHLY,
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 8, 9)
        );
    }

    public static InsurancePackage createValidInsurancePackageWithId(UUID id) {
        InsurancePackage insurancePackage = createValidInsurancePackage();
        ReflectionTestUtils.setField(insurancePackage, "id", id);
        return insurancePackage;
    }

    public static InsurancePackage createValidInsurancePackageWithStatus(InsurancePackageStatus status) {
        InsurancePackage insurancePackage = createValidInsurancePackage();
        insurancePackage.setStatus(status);
        return insurancePackage;
    }

    public static PackageRequest createValidPackageRequest() {
        return new PackageRequest(
                "Premium Health Package",
                Payroll.MONTHLY,
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 8, 9)
        );
    }

    public static PackageRequest createValidUpdatedPackageRequest(LocalDate startDate, LocalDate endDate) {
        return createValidPackageRequest()
                .name("Updated Package")
                .payroll(Payroll.MONTHLY)
                .startDate(startDate)
                .endDate(endDate);
    }

    public static PackageResponse createValidPackageResponse() {
        return new PackageResponse(
                UUID.randomUUID(),
                "Premium Health Package",
                Payroll.MONTHLY,
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 8, 9)
        );
    }

    public static String createValidPackageRequestBody(
            String name,
            String payroll,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return """
                {
                  "name": "%s",
                  "payroll": "%s",
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(name, payroll, startDate, endDate);
    }
}
