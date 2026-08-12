package com.cspot.insurahub.insurancepackage;

import com.cspot.insurahub.BaseIntegrationTest;
import com.cspot.insurahub.consumer.repository.ConsumerRepository;
import com.cspot.insurahub.enrollment.repository.EnrollmentRepository;
import com.cspot.insurahub.insurancepackage.entity.InsurancePackage;
import com.cspot.insurahub.insurancepackage.enumeration.InsurancePackageStatus;
import com.cspot.insurahub.insurancepackage.repository.InsurancePackageRepository;
import com.cspot.insurahub.payroll.Payroll;
import com.cspot.insurahub.plan.entity.InsurancePlan;
import com.cspot.insurahub.plan.repository.InsurancePlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static com.cspot.insurahub.consumer.testdata.ConsumerTestData.createUniqueValidConsumer;
import static com.cspot.insurahub.enrollment.testdata.EnrollmentTestData.createValidEnrollment;
import static com.cspot.insurahub.insurancepackage.testdata.InsurancePackageTestData.createValidPackageRequestBody;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidDentalBasicInsurancePlan;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidStandardHealthInsurancePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class PackageIntegrationTest extends BaseIntegrationTest {

    private static final String PACKAGES_ENDPOINT = "/packages";
    private static final String PACKAGE_NAME = "Premium Health Package";
    private static final String PERMISSIONS_CLAIM = "permissions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsurancePackageRepository repository;

    @Autowired
    private InsurancePlanRepository planRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ConsumerRepository consumerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void shouldCreatePackage() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageCreated(
                PACKAGE_NAME,
                "MONTHLY",
                startDate,
                endDate
        );
    }

    @Test
    void shouldGetCreatedPackage() throws Exception {
        String packageName = "Package For Get Test";
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageCreated(
                packageName,
                "MONTHLY",
                startDate,
                endDate
        );

        mockMvc.perform(get(PACKAGES_ENDPOINT)
                        .with(jwtWithPermissions("view:packages"))
                        .param("page", "0")
                        .param("size", "100")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(hasItem(packageName)))
                .andExpect(jsonPath("$.content[*].payroll").value(hasItem("MONTHLY")))
                .andExpect(jsonPath("$.content[*].startDate").value(hasItem(startDate.toString())))
                .andExpect(jsonPath("$.content[*].endDate").value(hasItem(endDate.toString())))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void shouldFilterPackagesByNameIgnoringCase() throws Exception {
        String uniqueValue = UUID.randomUUID().toString();
        String matchingPackageName = "Filterable Premium Package " + uniqueValue;
        String otherPackageName = "Unrelated Basic Package " + uniqueValue;
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        repository.saveAll(List.of(
                new InsurancePackage(
                        matchingPackageName,
                        Payroll.MONTHLY,
                        startDate,
                        startDate.plusMonths(1)
                ),
                new InsurancePackage(
                        otherPackageName,
                        Payroll.MONTHLY,
                        startDate,
                        startDate.plusMonths(1)
                )
        ));

        mockMvc.perform(get(PACKAGES_ENDPOINT)
                        .with(jwtWithPermissions("view:packages"))
                        .param("name", ("premium package " + uniqueValue).toUpperCase())
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(hasItem(matchingPackageName)))
                .andExpect(jsonPath("$.content[*].name").value(not(hasItem(otherPackageName))));
    }

    @Test
    void shouldReturnEmptyPageWhenPackageNameHasNoMatches() throws Exception {
        mockMvc.perform(get(PACKAGES_ENDPOINT)
                        .with(jwtWithPermissions("view:packages"))
                        .param("name", "missing-package-" + UUID.randomUUID())
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(empty()))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void shouldReturnAllPackagesWhenNameIsCleared() throws Exception {
        String uniqueValue = UUID.randomUUID().toString();
        String firstPackageName = "Cleared Name First Package " + uniqueValue;
        String secondPackageName = "Cleared Name Second Package " + uniqueValue;
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        repository.saveAll(List.of(
                new InsurancePackage(
                        firstPackageName,
                        Payroll.MONTHLY,
                        startDate,
                        startDate.plusMonths(1)
                ),
                new InsurancePackage(
                        secondPackageName,
                        Payroll.MONTHLY,
                        startDate,
                        startDate.plusMonths(1)
                )
        ));

        mockMvc.perform(get(PACKAGES_ENDPOINT)
                        .with(jwtWithPermissions("view:packages"))
                        .param("name", "")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(hasItems(
                        firstPackageName,
                        secondPackageName
                )));
    }

    @ParameterizedTest
    @MethodSource("exactPayrollPeriods")
    void shouldCreatePackageWithExactInclusivePayrollPeriod(
            String payroll,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        assertPackageCreated(
                PACKAGE_NAME,
                payroll,
                startDate,
                endDate
        );
    }

    @Test
    void shouldRejectPackageCreationWithoutAuthentication() throws Exception {
        long packagesBeforeRequest = repository.count();
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        mockMvc.perform(post(PACKAGES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPackageRequestBody(
                                PACKAGE_NAME,
                                "MONTHLY",
                                startDate,
                                endDate
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        assertEquals(packagesBeforeRequest, repository.count());
    }

    @Test
    void shouldRejectPackageCreationWithoutCreatePermission() throws Exception {
        long packagesBeforeRequest = repository.count();
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        mockMvc.perform(post(PACKAGES_ENDPOINT)
                        .with(jwtWithoutPermissions())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPackageRequestBody(
                                PACKAGE_NAME,
                                "MONTHLY",
                                startDate,
                                endDate
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(packagesBeforeRequest, repository.count());
    }

    @Test
    void shouldUpdatePackage() throws Exception {
        UUID packageId = repository.save(new InsurancePackage(
                "Original Package",
                Payroll.WEEKLY,
                LocalDate.now(clock).plusDays(1),
                LocalDate.now(clock).plusDays(7)
        )).getId();

        LocalDate updatedStartDate = LocalDate.now(clock).plusDays(2);
        LocalDate updatedEndDate = updatedStartDate.plusMonths(1);

        mockMvc.perform(put(PACKAGES_ENDPOINT + "/" + packageId)
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPackageRequestBody(
                                "Updated Package",
                                "MONTHLY",
                                updatedStartDate,
                                updatedEndDate
                        )))
                .andExpect(status().isNoContent());

        var updatedPackage = repository.findById(packageId)
                .orElseThrow(() -> new AssertionError("Package must exist after update"));
        assertEquals("Updated Package", updatedPackage.getName());
        assertEquals(Payroll.MONTHLY, updatedPackage.getPayroll());
        assertEquals(updatedStartDate, updatedPackage.getStartDate());
        assertEquals(updatedEndDate, updatedPackage.getEndDate());
    }

    @Test
    void shouldRejectPackageUpdateWithoutUpdatePermission() throws Exception {
        UUID packageId = repository.save(new InsurancePackage(
                "Original Package",
                Payroll.WEEKLY,
                LocalDate.now(clock).plusDays(1),
                LocalDate.now(clock).plusDays(7)
        )).getId();

        long packagesBeforeRequest = repository.count();

        mockMvc.perform(put(PACKAGES_ENDPOINT + "/" + packageId)
                        .with(jwtWithoutPermissions())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPackageRequestBody(
                                "Updated Package",
                                "MONTHLY",
                                LocalDate.now(clock).plusDays(2),
                                LocalDate.now(clock).plusDays(32)
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(packagesBeforeRequest, repository.count());
    }

    @Test
    void shouldRejectUpdateForMissingPackage() throws Exception {
        UUID missingPackageId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        mockMvc.perform(put(PACKAGES_ENDPOINT + "/" + missingPackageId)
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPackageRequestBody(
                                "Updated Package",
                                "MONTHLY",
                                startDate,
                                endDate
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PACKAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldRejectPackageUpdateWithInvalidDates() throws Exception {
        UUID packageId = repository.save(new InsurancePackage(
                "Original Package",
                Payroll.WEEKLY,
                LocalDate.now(clock).plusDays(1),
                LocalDate.now(clock).plusDays(7)
        )).getId();

        LocalDate startDate = LocalDate.now(clock).plusDays(2);
        LocalDate endDate = startDate.minusDays(1);

        mockMvc.perform(put(PACKAGES_ENDPOINT + "/" + packageId)
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPackageRequestBody(
                                "Updated Package",
                                "MONTHLY",
                                startDate,
                                endDate
                        )))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("PACKAGE_END_DATE_BEFORE_START_DATE"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @ParameterizedTest
    @MethodSource("periodsShorterThanPayrollCycle")
    void shouldRejectPackageWithPeriodShorterThanPayrollCycle(
            String payroll,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        assertPackageRejected(
                createValidPackageRequestBody(
                        PACKAGE_NAME,
                        payroll,
                        startDate,
                        endDate
                ),
                "PACKAGE_PERIOD_TOO_SHORT",
                422
        );
    }

    @Test
    void shouldRejectPackageWithoutName() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageRejected("""
                {
                  "payroll": "MONTHLY",
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(startDate, endDate),
                "VALIDATION_FAILED",
                400
        );
    }

    @Test
    void shouldRejectPackageWithBlankName() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageRejected(
                createValidPackageRequestBody(
                        "   ",
                        "MONTHLY",
                        startDate,
                        endDate
                ),
                "VALIDATION_FAILED",
                400
        );
    }

    @Test
    void shouldRejectPackageWithoutPayroll() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageRejected("""
                {
                  "name": "Premium Health Package",
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(startDate, endDate),
                "VALIDATION_FAILED",
                400
        );
    }

    @Test
    void shouldRejectPackageWithBlankPayroll() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageRejected(
                createValidPackageRequestBody(
                        PACKAGE_NAME,
                        "   ",
                        startDate,
                        endDate
                ),
                "MALFORMED_REQUEST_BODY",
                400
        );
    }

    @Test
    void shouldRejectPackageWithInvalidPayroll() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageRejected(
                createValidPackageRequestBody(
                        PACKAGE_NAME,
                        "DAILY",
                        startDate,
                        endDate
                ),
                "MALFORMED_REQUEST_BODY",
                400
        );
    }

    @Test
    void shouldRejectPackageWithNonStringPayroll() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        assertPackageRejected("""
                {
                  "name": "Premium Health Package",
                  "payroll": 123,
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(startDate, endDate),
                "MALFORMED_REQUEST_BODY",
                400
        );
    }

    @Test
    void shouldRejectPackageWithoutStartDate() throws Exception {
        LocalDate endDate = LocalDate.now(clock).plusMonths(1);

        assertPackageRejected("""
                {
                  "name": "Premium Health Package",
                  "payroll": "MONTHLY",
                  "endDate": "%s"
                }
                """.formatted(endDate),
                "VALIDATION_FAILED",
                400
        );
    }

    @Test
    void shouldRejectPackageWithInvalidStartDateFormat() throws Exception {
        LocalDate endDate = LocalDate.now(clock).plusMonths(1);

        assertPackageRejected("""
                {
                  "name": "Premium Health Package",
                  "payroll": "MONTHLY",
                  "startDate": "tomorrow",
                  "endDate": "%s"
                }
                """.formatted(endDate),
                "MALFORMED_REQUEST_BODY",
                400
        );
    }

    @Test
    void shouldRejectPackageWithoutEndDate() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        assertPackageRejected("""
                {
                  "name": "Premium Health Package",
                  "payroll": "MONTHLY",
                  "startDate": "%s"
                }
                """.formatted(startDate),
                "VALIDATION_FAILED",
                400
        );
    }

    @Test
    void shouldRejectPackageWithInvalidEndDateFormat() throws Exception {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        assertPackageRejected("""
                {
                  "name": "Premium Health Package",
                  "payroll": "MONTHLY",
                  "startDate": "%s",
                  "endDate": "next month"
                }
                """.formatted(startDate),
                "MALFORMED_REQUEST_BODY",
                400
        );
    }

    @Test
    void shouldRejectPackageWithMultipleInvalidBodyValues() throws Exception {
        LocalDate endDate = LocalDate.now(clock).plusMonths(1);

        assertPackageRejected("""
                {
                  "name": "   ",
                  "payroll": "DAILY",
                  "endDate": "%s"
                }
                """.formatted(endDate),
                "MALFORMED_REQUEST_BODY",
                400
        );
    }

    @Test
    void shouldRejectPackageWithUnsupportedContentType() throws Exception {
        long packagesBeforeRequest = repository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT)
                        .with(jwtWithPermissions("create:packages"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Premium Health Package"))
                .andExpect(status().isUnsupportedMediaType());

        assertEquals(packagesBeforeRequest, repository.count());
    }

    @Test
    void shouldRejectPackageWithoutRequestBody() throws Exception {
        assertPackageRejected(
                null,
                "MALFORMED_REQUEST_BODY",
                400
        );
    }

    @Test
    void shouldInitializePackage() throws Exception {
        InsurancePackage insurancePackage = savePackage();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/initialize")
                        .with(jwtWithPermissions("update:packages")))
                .andExpect(status().isNoContent());

        InsurancePackage updatedPackage = repository.findById(insurancePackage.getId()).orElseThrow();
        assertEquals(InsurancePackageStatus.INITIALIZED, updatedPackage.getStatus());
    }

    @Test
    void shouldRejectPackageInitializationWithoutAuthentication() throws Exception {
        InsurancePackage insurancePackage = savePackage();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/initialize"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        InsurancePackage updatedPackage = repository.findById(insurancePackage.getId()).orElseThrow();
        assertEquals(InsurancePackageStatus.NOT_STARTED, updatedPackage.getStatus());
    }

    @Test
    void shouldRejectPackageInitializationWithoutUpdatePermission() throws Exception {
        InsurancePackage insurancePackage = savePackage();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/initialize")
                        .with(jwtWithoutPermissions()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));

        InsurancePackage updatedPackage = repository.findById(insurancePackage.getId()).orElseThrow();
        assertEquals(InsurancePackageStatus.NOT_STARTED, updatedPackage.getStatus());
    }

    @Test
    void shouldRejectPackageInitializationWhenPackageDoesNotExist() throws Exception {
        UUID packageId = UUID.randomUUID();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + packageId + "/initialize")
                        .with(jwtWithPermissions("update:packages")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PACKAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldRejectAlreadyInitializedPackage() throws Exception {
        InsurancePackage insurancePackage = savePackage();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/initialize")
                        .with(jwtWithPermissions("update:packages")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/initialize")
                        .with(jwtWithPermissions("update:packages")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("PACKAGE_ALREADY_INITIALIZED"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void shouldRemovePackageAndConnectedPlans() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        List<InsurancePlan> plans = planRepository.saveAll(List.of(
                createValidStandardHealthInsurancePlan(insurancePackage),
                createValidDentalBasicInsurancePlan(insurancePackage)
        ));

        mockMvc.perform(delete(PACKAGES_ENDPOINT + "/" + insurancePackage.getId())
                        .with(jwtWithPermissions("delete:packages")))
                .andExpect(status().isNoContent());

        assertFalse(repository.existsById(insurancePackage.getId()));
        plans.forEach(plan -> assertFalse(planRepository.existsById(plan.getId())));
        assertSoftDeleted("packages", insurancePackage.getId());
        plans.forEach(plan -> assertSoftDeleted("plans", plan.getId()));
    }

    @Test
    void shouldRejectPackageRemovalWhenPackageIsInitialized() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        insurancePackage.setStatus(InsurancePackageStatus.INITIALIZED);
        repository.save(insurancePackage);

        mockMvc.perform(delete(PACKAGES_ENDPOINT + "/" + insurancePackage.getId())
                        .with(jwtWithPermissions("delete:packages")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("PACKAGE_REMOVAL_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value(
                        "Package removal is only allowed when the status is NOT_STARTED"
                ));

        assertTrue(repository.existsById(insurancePackage.getId()));
    }

    @Test
    void shouldRejectPackageRemovalWhenAnyPlanHasEnrollment() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        InsurancePlan plan = planRepository.save(createValidStandardHealthInsurancePlan(insurancePackage));
        enrollmentRepository.save(createValidEnrollment(consumerRepository.save(createUniqueValidConsumer()), plan));

        mockMvc.perform(delete(PACKAGES_ENDPOINT + "/" + insurancePackage.getId())
                        .with(jwtWithPermissions("delete:packages")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("PACKAGE_PLANS_HAVE_ENROLLMENTS"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value(
                        "Package removal is only allowed when its plans have no enrollments"
                ));

        assertTrue(repository.existsById(insurancePackage.getId()));
        assertTrue(planRepository.existsById(plan.getId()));
    }

    private void assertPackageCreated(
            String name,
            String payroll,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        long packagesBeforeRequest = repository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT)
                        .with(jwtWithPermissions("create:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPackageRequestBody(
                                name,
                                payroll,
                                startDate,
                                endDate
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        assertEquals(packagesBeforeRequest + 1, repository.count());
    }

    private void assertPackageRejected(
            String requestBody,
            String expectedCode,
            int expectedStatus
    ) throws Exception {
        long packagesBeforeRequest = repository.count();

        var request = post(PACKAGES_ENDPOINT)
                .with(jwtWithPermissions("create:packages"))
                .contentType(MediaType.APPLICATION_JSON);

        if (requestBody != null) {
            request.content(requestBody);
        }

        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.error").value(expectedCode))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.message").exists());

        assertEquals(packagesBeforeRequest, repository.count());
    }

    private InsurancePackage savePackage() {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        return repository.save(new InsurancePackage(
                PACKAGE_NAME,
                Payroll.MONTHLY,
                startDate,
                startDate.plusMonths(1)
        ));
    }

    private void assertSoftDeleted(String tableName, UUID id) {
        Integer deletedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName
                        + " WHERE id = ? AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL",
                Integer.class,
                id
        );
        assertEquals(1, deletedRows);
    }

    private RequestPostProcessor jwtWithPermissions(String... permissions) {
        return jwt()
                .jwt(jwt -> jwt.claim(
                        PERMISSIONS_CLAIM,
                        List.of(permissions)
                ))
                .authorities(this::convertAuthorities);
    }

    private RequestPostProcessor jwtWithoutPermissions() {
        return jwt()
                .jwt(jwt -> jwt.claim(
                        PERMISSIONS_CLAIM,
                        List.of()
                ))
                .authorities(this::convertAuthorities);
    }

    private Collection<GrantedAuthority> convertAuthorities(Jwt jwt) {
        return Objects.requireNonNull(
                jwtAuthenticationConverter.convert(jwt),
                "JWT authentication conversion must not return null"
        ).getAuthorities();
    }

    private Stream<Arguments> exactPayrollPeriods() {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        return Stream.of(
                Arguments.of(
                        "WEEKLY",
                        startDate,
                        startDate.plusDays(6)
                ),
                Arguments.of(
                        "BI_WEEKLY",
                        startDate,
                        startDate.plusDays(13)
                ),
                Arguments.of(
                        "MONTHLY",
                        startDate,
                        startDate.plusMonths(1).minusDays(1)
                )
        );
    }

    private Stream<Arguments> periodsShorterThanPayrollCycle() {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        return Stream.of(
                Arguments.of(
                        "WEEKLY",
                        startDate,
                        startDate.plusDays(5)
                ),
                Arguments.of(
                        "BI_WEEKLY",
                        startDate,
                        startDate.plusDays(12)
                ),
                Arguments.of(
                        "MONTHLY",
                        startDate,
                        startDate.plusMonths(1).minusDays(2)
                )
        );
    }

}
