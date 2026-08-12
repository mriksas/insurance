package com.cspot.insurahub.plan;

import com.cspot.insurahub.BaseIntegrationTest;
import com.cspot.insurahub.insurancepackage.entity.InsurancePackage;
import com.cspot.insurahub.insurancepackage.enumeration.InsurancePackageStatus;
import com.cspot.insurahub.insurancepackage.repository.InsurancePackageRepository;
import com.cspot.insurahub.model.PlanType;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidDentalBasicInsurancePlan;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidStandardHealthInsurancePlan;
import static com.cspot.insurahub.plan.testdata.PlanTestData.createValidPlanRequestBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class PlanIntegrationTest extends BaseIntegrationTest {

    private static final String PACKAGES_ENDPOINT = "/packages";
    private static final String PLANS_ENDPOINT = "/plans";
    private static final String PACKAGE_NAME = "Premium Health Package";
    private static final String PERMISSIONS_CLAIM = "permissions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsurancePackageRepository packageRepository;

    @Autowired
    private InsurancePlanRepository planRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void shouldAddPlanToPackage() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        long plansBeforeRequest = planRepository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Standard Health",
                                "HEALTH_INSURANCE",
                                250,
                                500
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        assertThat(planRepository.count())
                .isEqualTo(plansBeforeRequest + 1);
    }

    @Test
    void shouldRejectAddPlanToInitializedPackage() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        insurancePackage.setStatus(InsurancePackageStatus.INITIALIZED);
        packageRepository.save(insurancePackage);
        long plansBeforeRequest = planRepository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Vision Plus",
                                "VISION_INSURANCE",
                                120,
                                300
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PACKAGE_UPDATE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(planRepository.count())
                .isEqualTo(plansBeforeRequest);
    }

    @Test
    void shouldRejectAddPlanWithoutAuthentication() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        long plansBeforeRequest = planRepository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Standard Health",
                                "HEALTH_INSURANCE",
                                250,
                                500
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        assertThat(planRepository.count())
                .isEqualTo(plansBeforeRequest);
    }

    @Test
    void shouldRejectAddPlanWithoutUpdatePermission() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        long plansBeforeRequest = planRepository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .with(jwtWithoutPermissions())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Standard Health",
                                "HEALTH_INSURANCE",
                                250,
                                500
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));

        assertThat(planRepository.count())
                .isEqualTo(plansBeforeRequest);
    }

    @Test
    void shouldRejectAddPlanWhenPackageDoesNotExist() throws Exception {
        UUID packageId = UUID.randomUUID();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + packageId + "/plans")
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Standard Health",
                                "HEALTH_INSURANCE",
                                250,
                                500
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PACKAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldUpdatePlan() throws Exception {
        InsurancePlan plan = savePlan(savePackage());

        mockMvc.perform(put(PLANS_ENDPOINT + "/" + plan.getId())
                        .with(jwtWithPermissions("update:plans"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Updated Dental",
                                "DENTAL_INSURANCE",
                                300,
                                600
                        )))
                .andExpect(status().isNoContent());

        InsurancePlan updatedPlan = planRepository.findById(plan.getId())
                .orElseThrow(() -> new AssertionError("Plan must exist after update"));

        assertThat(updatedPlan.getName()).isEqualTo("Updated Dental");
        assertThat(updatedPlan.getType()).isEqualTo(PlanType.DENTAL_INSURANCE);
        assertThat(updatedPlan.getContribution())
                .isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(updatedPlan.getElection())
                .isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    void shouldRejectUpdatePlanWhenPlanDoesNotExist() throws Exception {
        UUID planId = UUID.randomUUID();

        mockMvc.perform(put(PLANS_ENDPOINT + "/" + planId)
                        .with(jwtWithPermissions("update:plans"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Updated Dental",
                                "DENTAL_INSURANCE",
                                300,
                                600
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("InsurancePlan with id '" + planId + "' was not found."));
    }

    @Test
    void shouldRejectUpdatePlanWithoutAuthentication() throws Exception {
        InsurancePlan plan = savePlan(savePackage());

        mockMvc.perform(put(PLANS_ENDPOINT + "/" + plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Updated Dental",
                                "DENTAL_INSURANCE",
                                300,
                                600
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        assertPlanUnchanged(plan);
    }

    @Test
    void shouldRejectUpdatePlanWithoutUpdatePermission() throws Exception {
        InsurancePlan plan = savePlan(savePackage());

        mockMvc.perform(put(PLANS_ENDPOINT + "/" + plan.getId())
                        .with(jwtWithoutPermissions())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Updated Dental",
                                "DENTAL_INSURANCE",
                                300,
                                600
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));

        assertPlanUnchanged(plan);
    }

    @Test
    void shouldRejectUpdatePlanOfInitializedPackage() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        insurancePackage.setStatus(InsurancePackageStatus.INITIALIZED);
        packageRepository.save(insurancePackage);
        InsurancePlan plan = savePlan(insurancePackage);

        mockMvc.perform(put(PLANS_ENDPOINT + "/" + plan.getId())
                        .with(jwtWithPermissions("update:plans"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Updated Dental",
                                "DENTAL_INSURANCE",
                                300,
                                600
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PACKAGE_UPDATE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(400));

        assertPlanUnchanged(plan);
    }

    @Test
    void shouldRejectAddPlanWithoutRequiredFields() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        long plansBeforeRequest = planRepository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());

        assertThat(planRepository.count())
                .isEqualTo(plansBeforeRequest);
    }

    @ParameterizedTest
    @MethodSource("invalidPlanAmounts")
    void shouldRejectPlanAmountsOutsideAllowedRange(
            int contribution,
            int election
    ) throws Exception {
        InsurancePackage insurancePackage = savePackage();
        long plansBeforeRequest = planRepository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Standard Health",
                                "HEALTH_INSURANCE",
                                contribution,
                                election
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        assertThat(planRepository.count())
                .isEqualTo(plansBeforeRequest);
    }

    @Test
    void shouldRejectPlanNameWithInvalidCharacters() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        long plansBeforeRequest = planRepository.count();

        mockMvc.perform(post(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .with(jwtWithPermissions("update:packages"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidPlanRequestBody(
                                "Plan!",
                                "HEALTH_INSURANCE",
                                250,
                                500
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        assertThat(planRepository.count())
                .isEqualTo(plansBeforeRequest);
    }

    @Test
    void shouldGetPlansOfPackage() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        planRepository.saveAll(List.of(
                createValidStandardHealthInsurancePlan(insurancePackage),
                createValidDentalBasicInsurancePlan(insurancePackage)
        ));

        mockMvc.perform(get(PACKAGES_ENDPOINT + "/" + insurancePackage.getId() + "/plans")
                        .with(jwtWithPermissions("view:packages")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content.[*].name").value(hasItems("Standard Health", "Dental Basic")))
                .andExpect(jsonPath("$.content.[*].type").value(hasItems("HEALTH_INSURANCE", "DENTAL_INSURANCE")))
                .andExpect(jsonPath("$.content.[*].contribution").value(hasItems(250, 100)))
                .andExpect(jsonPath("$.content.[*].election").value(hasItems(500, 300)))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void shouldDeletePlan() throws Exception {
        InsurancePlan plan = savePlan(savePackage());

        mockMvc.perform(delete(PLANS_ENDPOINT + "/" + plan.getId())
                        .with(jwtWithPermissions("delete:plans")))
                .andExpect(status().isNoContent());

        assertThat(planRepository.existsById(plan.getId())).isFalse();
        assertSoftDeleted(plan.getId());
    }

    @Test
    void shouldRejectDeletingPlanWhenPackageIsInitialized() throws Exception {
        InsurancePackage insurancePackage = savePackage();
        insurancePackage.setStatus(InsurancePackageStatus.INITIALIZED);
        packageRepository.save(insurancePackage);
        InsurancePlan plan = savePlan(insurancePackage);

        mockMvc.perform(delete(PLANS_ENDPOINT + "/" + plan.getId())
                        .with(jwtWithPermissions("delete:plans")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PACKAGE_UPDATE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(planRepository.existsById(plan.getId())).isTrue();
    }

    @Test
    void shouldRejectDeletingPlanWhenPlanDoesNotExist() throws Exception {
        UUID planId = UUID.randomUUID();

        mockMvc.perform(delete(PLANS_ENDPOINT + "/" + planId)
                        .with(jwtWithPermissions("delete:plans")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    private InsurancePackage savePackage() {
        LocalDate startDate = LocalDate.now(clock).plusDays(1);

        return packageRepository.save(new InsurancePackage(
                PACKAGE_NAME,
                Payroll.MONTHLY,
                startDate,
                startDate.plusMonths(1)
        ));
    }

    private InsurancePlan savePlan(InsurancePackage insurancePackage) {
        return planRepository.save(createValidStandardHealthInsurancePlan(insurancePackage));
    }

    private void assertPlanUnchanged(InsurancePlan plan) {
        InsurancePlan savedPlan = planRepository.findById(plan.getId())
                .orElseThrow(() -> new AssertionError("Plan must exist after rejected update"));

        assertThat(savedPlan.getName()).isEqualTo(plan.getName());
        assertThat(savedPlan.getType()).isEqualTo(plan.getType());
        assertThat(savedPlan.getContribution())
                .isEqualByComparingTo(plan.getContribution());
        assertThat(savedPlan.getElection())
                .isEqualByComparingTo(plan.getElection());
    }

    private void assertSoftDeleted(UUID planId) {
        Integer deletedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM plans "
                        + "WHERE id = ? AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL",
                Integer.class,
                planId
        );
        assertThat(deletedRows).isEqualTo(1);
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

    private Stream<Arguments> invalidPlanAmounts() {
        return Stream.of(
                Arguments.of(9, 500),
                Arguments.of(1001, 500),
                Arguments.of(250, 9),
                Arguments.of(250, 1001)
        );
    }
}
