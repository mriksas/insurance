package com.cspot.insurahub.claim.controller;

import com.cspot.insurahub.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@ActiveProfiles("test")
class ClaimControllerIntegrationTest extends BaseIntegrationTest {

    private static final String CONSUMERS_SEED = "/consumer/seed-consumers.sql";
    private static final String PACKAGES_SEED = "/package/seed-packages.sql";
    private static final String PLANS_SEED = "/plan/seed-plans.sql";
    private static final String ENROLLMENTS_SEED = "/enrollment/seed-enrollments.sql";
    private static final String CLAIMS_SEED = "/claim/seed-claims.sql";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void approveClaimShouldReturnNoContentForPendingClaim() throws Exception {
        UUID claimId = seedClaim("PENDING");

        mockMvc.perform(post("/claims/{claimId}/approve", claimId)
                        .with(adminUser()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM claims WHERE id = ?", String.class, claimId);
        assertThat(status).isEqualTo("APPROVED");
    }

    @Test
    void approveClaimShouldReturnUnprocessableEntityForAlreadyApprovedClaim() throws Exception {
        UUID claimId = seedClaim("APPROVED");

        mockMvc.perform(post("/claims/{claimId}/approve", claimId)
                        .with(adminUser()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("CLAIM_NOT_PENDING"));
    }

    @Test
    void approveClaimShouldReturnUnprocessableEntityForRejectedClaim() throws Exception {
        UUID claimId = seedClaim("REJECTED");

        mockMvc.perform(post("/claims/{claimId}/approve", claimId)
                        .with(adminUser()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("CLAIM_NOT_PENDING"));
    }

    @Test
    void approveClaimShouldReturnNotFoundForNonExistentClaim() throws Exception {
        UUID nonExistentClaimId = UUID.randomUUID();

        mockMvc.perform(post("/claims/{claimId}/approve", nonExistentClaimId)
                        .with(adminUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void approveClaimShouldReturnUnauthorizedWithoutToken() throws Exception {
        UUID claimId = seedClaim("PENDING");

        mockMvc.perform(post("/claims/{claimId}/approve", claimId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveClaimShouldReturnForbiddenWithoutPermission() throws Exception {
        UUID claimId = seedClaim("PENDING");

        mockMvc.perform(post("/claims/{claimId}/approve", claimId)
                        .with(jwtWithPermissions()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnClaims() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("page", "0")
                        .param("size", "2")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("cccccccc-0001-0001-0001-000000000001"))
                .andExpect(jsonPath("$.content[0].claimNumber").value("LT20260715001"))
                .andExpect(jsonPath("$.content[0].consumerId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].consumerFullName").value("First Consumer"))
                .andExpect(jsonPath("$.content[0].serviceDate").value("2026-07-15"))
                .andExpect(jsonPath("$.content[0].planId").value("bbbbbbbb-0001-0001-0001-000000000001"))
                .andExpect(jsonPath("$.content[0].planName").value("Standard Health"))
                .andExpect(jsonPath("$.content[0].amount").value(285.5))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[1].consumerId").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.content[1].consumerFullName").value("Second Consumer"))
                .andExpect(jsonPath("$.content[1].claimNumber").value("LT20260714001"))
                .andExpect(jsonPath("$.content[1].planId").value("bbbbbbbb-0002-0002-0002-000000000002"))
                .andExpect(jsonPath("$.content[1].planName").value("Dental Care"))
                .andExpect(jsonPath("$.content[1].status").value("APPROVED"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnRequestedClaimPage() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("page", "1")
                        .param("size", "2")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("cccccccc-0003-0003-0003-000000000003"))
                .andExpect(jsonPath("$.content[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.content[1].id").value("cccccccc-0004-0004-0004-000000000004"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnClaimsSortedByServiceDateDescByDefault() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("cccccccc-0001-0001-0001-000000000001"))
                .andExpect(jsonPath("$.content[1].id").value("cccccccc-0002-0002-0002-000000000002"))
                .andExpect(jsonPath("$.content[2].id").value("cccccccc-0003-0003-0003-000000000003"))
                .andExpect(jsonPath("$.content[3].id").value("cccccccc-0004-0004-0004-000000000004"))
                .andExpect(jsonPath("$.content[4].id").value("cccccccc-0005-0005-0005-000000000005"));
    }

    @Test
    void shouldReturnEmptyClaimList() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.totalPages").value(0));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnClaimsForAuthenticatedConsumerOnly() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(jwtWithPermission("view:own:claims", "auth0|consumer-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("cccccccc-0001-0001-0001-000000000001"))
                .andExpect(jsonPath("$.content[0].claimNumber").value("LT20260715001"))
                .andExpect(jsonPath("$.content[0].consumerId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].consumerFullName").value("First Consumer"))
                .andExpect(jsonPath("$.content[0].serviceDate").value("2026-07-15"))
                .andExpect(jsonPath("$.content[0].lastUpdateDate").value("2026-07-15"))
                .andExpect(jsonPath("$.content[0].planId").value("bbbbbbbb-0001-0001-0001-000000000001"))
                .andExpect(jsonPath("$.content[0].planName").value("Standard Health"))
                .andExpect(jsonPath("$.content[0].amount").value(285.5))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[1].id").value("cccccccc-0004-0004-0004-000000000004"))
                .andExpect(jsonPath("$.content[1].claimNumber").value("LT20260712001"))
                .andExpect(jsonPath("$.content[1].consumerId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[1].consumerFullName").value("First Consumer"))
                .andExpect(jsonPath("$.content[1].serviceDate").value("2026-07-12"))
                .andExpect(jsonPath("$.content[1].lastUpdateDate").value("2026-07-12"))
                .andExpect(jsonPath("$.content[1].planId").value("bbbbbbbb-0002-0002-0002-000000000002"))
                .andExpect(jsonPath("$.content[1].planName").value("Dental Care"))
                .andExpect(jsonPath("$.content[1].amount").value(430.0))
                .andExpect(jsonPath("$.content[1].status").value("PENDING"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    @Sql(CONSUMERS_SEED)
    void shouldReturnEmptyClaimPageForConsumerWithoutClaims() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(jwtWithPermission("view:own:claims", "auth0|consumer-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.totalPages").value(0));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnAllClaimsForAdminEvenWhenSubjectBelongsToConsumer() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(jwtWithPermission("view:claims", "auth0|consumer-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(5));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldRejectClaimListWithoutAuthority() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldAcceptAllowedSortProperty() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("sort", "amount,asc")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk());
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldAcceptClaimNumberSortProperty() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("sort", "claimNumber,asc")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk());
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnClaimsSearchedByClaimNumber() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("claimNumber", "LT20260714")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value("cccccccc-0002-0002-0002-000000000002"))
                .andExpect(jsonPath("$.content[0].claimNumber").value("LT20260714001"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnClaimsSearchedByConsumer() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("consumer", "First Consumer")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].consumerFullName").value("First Consumer"))
                .andExpect(jsonPath("$.content[1].consumerFullName").value("First Consumer"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldSearchClaimsByConsumerCaseSensitively() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("consumer", "first consumer")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnClaimsSearchedByClaimNumberAndConsumer() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("claimNumber", "LT20260712")
                        .param("consumer", "First Consumer")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value("cccccccc-0004-0004-0004-000000000004"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldReturnEmptyClaimsWhenSearchDoesNotMatch() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("claimNumber", "LT999")
                        .param("consumer", "First Consumer")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.totalPages").value(0));
    }

    @Test
    @Sql({CONSUMERS_SEED, PACKAGES_SEED, PLANS_SEED, ENROLLMENTS_SEED, CLAIMS_SEED})
    void shouldRejectUnsupportedSortDirection() throws Exception {
        mockMvc.perform(get("/claims")
                        .param("sort", "createdAt,ascending")
                        .with(jwtWithPermission("view:claims")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("sort direction must be asc or desc"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/claims"));
    }

    private RequestPostProcessor adminUser() {
        return jwtWithPermissions("update:claims");
    }

    private RequestPostProcessor jwtWithPermission(String permission) {
        return jwtWithPermission(permission, "auth0|admin");
    }

    private RequestPostProcessor jwtWithPermissions(String... permissions) {
        return jwtWithPermissionsForSubject("auth0|admin", permissions);
    }

    private RequestPostProcessor jwtWithPermission(String permission, String subject) {
        return jwtWithPermissionsForSubject(subject, permission);
    }

    private RequestPostProcessor jwtWithPermissionsForSubject(String subject, String... permissions) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(subject)
                        .claim("permissions", List.of(permissions)))
                .authorities(jwt -> Objects.requireNonNull(jwtAuthenticationConverter.convert(jwt)).getAuthorities());
    }

    private UUID seedClaim(String status) {
        UUID consumerId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();

        String personalId = UUID.randomUUID().toString().substring(0, 11);
        String email = "claimtest-" + UUID.randomUUID() + "@test.com";

        jdbcTemplate.update(
                "INSERT INTO consumers (id, version, idp_id, email, first_name, last_name, "
                        + "personal_id, date_of_birth, address, city, created_at, created_by, deleted_at) "
                        + "VALUES (?, 0, ?, ?, 'Test', 'User', ?, '2000-01-01', '123 Test St', 'Testville', "
                        + "NOW(), 'test', NULL)",
                consumerId, "auth0|claim-consumer-" + consumerId, email, personalId);

        jdbcTemplate.update(
                "INSERT INTO packages (id, version, name, payroll, start_date, end_date, status, "
                        + "created_at, created_by, deleted_at) "
                        + "VALUES (?, 0, 'Test Package', 'MONTHLY', '2024-01-01', '2025-01-01', "
                        + "'INITIALIZED', NOW(), 'test', NULL)",
                packageId);

        jdbcTemplate.update(
                "INSERT INTO plans (id, version, package_id, name, type, contribution, election, "
                        + "created_at, created_by, deleted_at) "
                        + "VALUES (?, 0, ?, 'Test Plan', 'HEALTH_INSURANCE', 100.00, 50.00, "
                        + "NOW(), 'test', NULL)",
                planId, packageId);

        jdbcTemplate.update(
                "INSERT INTO enrollments (id, version, consumer_id, plan_id, status, "
                        + "created_at, created_by, deleted_at) "
                        + "VALUES (?, 0, ?, ?, 'ACTIVE', NOW(), 'test', NULL)",
                enrollmentId, consumerId, planId);

        jdbcTemplate.update(
                "INSERT INTO claims (id, version, enrollment_id, service_date, amount, status, "
                        + "created_at, created_by, deleted_at) "
                        + "VALUES (?, 0, ?, '2026-07-01', 100.00, ?, NOW(), 'test', NULL)",
                claimId, enrollmentId, status);

        return claimId;
    }
}
