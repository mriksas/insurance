package com.cspot.insurahub.claim.service;

import com.cspot.insurahub.claim.entity.Claim;
import com.cspot.insurahub.claim.entity.Receipt;
import com.cspot.insurahub.claim.enumeration.ClaimStatus;
import com.cspot.insurahub.claim.exception.ClaimNotPendingException;
import com.cspot.insurahub.claim.mapper.ClaimMapper;
import com.cspot.insurahub.claim.repository.ClaimRepository;
import com.cspot.insurahub.claim.repository.ReceiptRepository;
import com.cspot.insurahub.claim.storage.PostgresReceiptStorage;
import com.cspot.insurahub.common.exception.ResourceNotFoundException;
import com.cspot.insurahub.consumer.service.IdpIdMappingService;
import com.cspot.insurahub.enrollment.entity.Enrollment;
import com.cspot.insurahub.enrollment.repository.EnrollmentRepository;
import com.cspot.insurahub.model.ClaimResponse;
import com.cspot.insurahub.model.PostClaimRequest;
import com.cspot.insurahub.model.PostResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cspot.insurahub.claim.testdata.ClaimTestData.createValidClaim;
import static com.cspot.insurahub.claim.testdata.ClaimTestData.createValidClaimForListResponse;
import static com.cspot.insurahub.claim.testdata.ClaimTestData.createValidClaimResponse;
import static com.cspot.insurahub.claim.testdata.ClaimTestData.createValidPostClaimRequest;
import static com.cspot.insurahub.enrollment.testdata.EnrollmentTestData.createValidEnrollmentWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PostgresReceiptStorage receiptStorage;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private ClaimMapper claimMapper;

    @Mock
    private IdpIdMappingService idpIdMappingService;

    @InjectMocks
    private ClaimService claimService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateClaim() {
        UUID enrollmentId = UUID.randomUUID();
        Enrollment enrollment = createValidEnrollmentWithId(enrollmentId);

        PostClaimRequest request = createValidPostClaimRequest(enrollmentId);

        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(enrollment));

        UUID claimId = UUID.randomUUID();
        doAnswer(invocation -> {
            Claim claim = invocation.getArgument(0);
            ReflectionTestUtils.setField(claim, "id", claimId);
            return claim;
        }).when(claimRepository).save(any(Claim.class));

        Receipt receipt = mock(Receipt.class);
        when(receipt.getId()).thenReturn(UUID.randomUUID());

        when(receiptStorage.store(any(Claim.class), same(multipartFile)))
                .thenReturn(receipt);

        PostResponse response = claimService.createClaim(request, multipartFile);

        assertThat(response.getId()).isEqualTo(claimId);

        verify(enrollmentRepository).findById(enrollmentId);

        ArgumentCaptor<Claim> claimCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(claimCaptor.capture());
        Claim savedClaim = claimCaptor.getValue();

        assertThat(savedClaim.getEnrollment()).isSameAs(enrollment);
        assertThat(savedClaim.getServiceDate()).isEqualTo(request.getServiceDate());
        assertThat(savedClaim.getClaimNumber()).isNull();
        assertThat(savedClaim.getAmount()).isEqualByComparingTo(request.getAmount());
        assertThat(savedClaim.getStatus()).isEqualTo(ClaimStatus.PENDING);

        verify(receiptStorage).store(same(savedClaim), same(multipartFile));
        verifyNoMoreInteractions(receiptRepository);
    }

    @Test
    void shouldThrowWhenEnrollmentDoesNotExist() {
        UUID enrollmentId = UUID.randomUUID();
        PostClaimRequest request = createValidPostClaimRequest(enrollmentId);

        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> claimService.createClaim(request, multipartFile)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Enrollment not found with id: " + enrollmentId);

        verify(enrollmentRepository).findById(enrollmentId);
        verifyNoInteractions(
                claimRepository,
                receiptRepository,
                receiptStorage
        );
    }

    @Test
    void shouldReturnReceiptContent() throws IOException {
        UUID claimId = UUID.randomUUID();
        byte[] content = "receipt".getBytes();
        Receipt receipt = new Receipt(
                createValidClaim(),
                "receipt.pdf",
                "application/pdf",
                (long) content.length,
                content
        );

        when(receiptRepository.findByClaimId(claimId))
                .thenReturn(Optional.of(receipt));

        Resource resource = claimService.getReceipt(claimId);

        assertThat(resource.getInputStream().readAllBytes())
                .containsExactly(content);

        verify(receiptRepository).findByClaimId(claimId);
        verifyNoInteractions(
                claimRepository,
                enrollmentRepository,
                receiptStorage
        );
    }

    @Test
    void shouldThrowWhenReceiptDoesNotExist() {
        UUID claimId = UUID.randomUUID();

        when(receiptRepository.findByClaimId(claimId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> claimService.getReceipt(claimId)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Receipt not found with claim id: " + claimId);

        verify(receiptRepository).findByClaimId(claimId);
        verifyNoInteractions(
                claimRepository,
                enrollmentRepository,
                receiptStorage
        );
    }

    @Test
    void shouldApprovePendingClaim() {
        UUID claimId = UUID.randomUUID();
        Claim claim = createValidClaim();

        when(claimRepository.findByIdOrThrow(claimId)).thenReturn(claim);

        claimService.approveClaim(claimId);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        verify(claimRepository).findByIdOrThrow(claimId);
        verifyNoInteractions(
                receiptRepository,
                enrollmentRepository,
                receiptStorage,
                multipartFile
        );
    }

    @Test
    void shouldThrowWhenClaimToApproveDoesNotExist() {
        UUID claimId = UUID.randomUUID();

        when(claimRepository.findByIdOrThrow(claimId))
                .thenThrow(new ResourceNotFoundException(Claim.class, claimId));

        assertThrows(
                ResourceNotFoundException.class,
                () -> claimService.approveClaim(claimId)
        );

        verify(claimRepository).findByIdOrThrow(claimId);
        verifyNoInteractions(
                receiptRepository,
                enrollmentRepository,
                receiptStorage,
                multipartFile
        );
    }

    @Test
    void shouldThrowWhenApprovingNonPendingClaim() {
        UUID claimId = UUID.randomUUID();
        Claim claim = createValidClaim();
        claim.setStatus(ClaimStatus.REJECTED);

        when(claimRepository.findByIdOrThrow(claimId)).thenReturn(claim);

        ClaimNotPendingException exception = assertThrows(
                ClaimNotPendingException.class,
                () -> claimService.approveClaim(claimId)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Claim with id '" + claimId + "' cannot be approved because it is not pending. "
                        + "Current status: REJECTED");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        verify(claimRepository).findByIdOrThrow(claimId);
        verifyNoInteractions(
                receiptRepository,
                enrollmentRepository,
                receiptStorage,
                multipartFile
        );
    }

    @Test
    void shouldGetClaims() {
        Claim claim = createValidClaimForListResponse();
        ClaimResponse listItem = createValidClaimResponse();
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").ascending());
        authenticateWith("view:claims");
        when(claimRepository.findAll(anyClaimSpecification(), same(pageable)))
                .thenReturn(new PageImpl<>(List.of(claim), PageRequest.of(0, 20), 1));
        when(claimMapper.toListItemResponse(claim)).thenReturn(listItem);

        Page<ClaimResponse> claims = claimService.getClaims(null, null, pageable);

        assertThat(claims.getContent()).containsExactly(listItem);
        verify(claimRepository).findAll(anyClaimSpecification(), same(pageable));
        verify(claimMapper).toListItemResponse(claim);
        verifyNoInteractions(receiptRepository, enrollmentRepository,
                receiptStorage, multipartFile);
    }

    @Test
    void shouldReturnEmptyPageWhenNoClaims() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").ascending());
        authenticateWith("view:claims");
        when(claimRepository.findAll(anyClaimSpecification(), same(pageable)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        Page<ClaimResponse> claims = claimService.getClaims(" ", "", pageable);

        assertThat(claims.getContent()).isEmpty();
        verify(claimRepository).findAll(anyClaimSpecification(), same(pageable));
        verifyNoInteractions(claimMapper, receiptRepository, enrollmentRepository,
                receiptStorage, multipartFile);
    }

    @Test
    void shouldSearchClaims() {
        Claim claim = createValidClaimForListResponse();
        ClaimResponse listItem = createValidClaimResponse();
        Pageable pageable = PageRequest.of(0, 20, Sort.by("serviceDate").descending());
        authenticateWith("view:claims");
        when(claimRepository.findAll(anyClaimSpecification(), same(pageable)))
                .thenReturn(new PageImpl<>(List.of(claim), pageable, 1));
        when(claimMapper.toListItemResponse(claim)).thenReturn(listItem);

        Page<ClaimResponse> claims = claimService.getClaims(" LT20260715 ", " John Doe ", pageable);

        assertThat(claims.getContent()).containsExactly(listItem);
        verify(claimRepository).findAll(anyClaimSpecification(), same(pageable));
        verify(claimMapper).toListItemResponse(claim);
        verifyNoInteractions(receiptRepository, enrollmentRepository,
                receiptStorage, multipartFile);
    }

    private void authenticateWith(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "subject",
                "credentials",
                List.of(new SimpleGrantedAuthority(authority))
        ));
    }

    private Specification<Claim> anyClaimSpecification() {
        return any();
    }

}
