package com.cspot.insurahub.claim.service;

import com.cspot.insurahub.claim.entity.Claim;
import com.cspot.insurahub.claim.entity.Receipt;
import com.cspot.insurahub.claim.enumeration.ClaimStatus;
import com.cspot.insurahub.claim.exception.ClaimNotPendingException;
import com.cspot.insurahub.claim.exception.ClaimUpdateNotAllowedException;
import com.cspot.insurahub.claim.filter.ClaimSpecification;
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
import com.cspot.insurahub.model.UpdateClaimRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ReceiptRepository receiptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PostgresReceiptStorage receiptStorage;
    private final ClaimMapper claimMapper;
    private final IdpIdMappingService idpIdMappingService;

    @Transactional(readOnly = true)
    public Page<ClaimResponse> getClaims(String claimNumber, String consumer, Pageable pageable) {
        String cleanClaimNumber = cleanSearchTerm(claimNumber);
        String cleanConsumer = cleanSearchTerm(consumer);

        Specification<Claim> specification = buildSpecification(cleanClaimNumber, cleanConsumer);
        if (hasAuthority("view:claims")) {
            return claimRepository.findAll(specification, pageable).map(claimMapper::toListItemResponse);
        } else if (hasAuthority("view:own:claims")) {
            UUID consumerId = idpIdMappingService.getCurrentAuthenticatedConsumerId();
            specification = specification.and(ClaimSpecification.byConsumerId(consumerId));
            return claimRepository.findAll(specification, pageable).map(claimMapper::toListItemResponse);
        } else {
            throw new AccessDeniedException("Missing required authority to view claims");
        }
    }

    private Specification<Claim> buildSpecification(String claimNumber, String consumer) {
        Specification<Claim> specification = ClaimSpecification.withDetails();
        if (claimNumber != null) {
            specification = specification.and(ClaimSpecification.claimNumberContains(claimNumber));
        }
        if (consumer != null) {
            specification = specification.and(ClaimSpecification.consumerFullNameContains(consumer));
        }
        return specification;
    }

    private String cleanSearchTerm(String searchTerm) {
        return searchTerm == null || searchTerm.isBlank()
                ? null
                : searchTerm.trim();
    }

    @Transactional
    public PostResponse createClaim(PostClaimRequest request, MultipartFile receipt) {
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found with id: " + request.getEnrollmentId()
                ));

        Claim claim = new Claim(
                enrollment,
                request.getServiceDate(),
                request.getAmount()
        );

        log.debug(
                "Creating Claim: enrollmentId={}, serviceDate={}, amount={}",
                enrollment.getId(),
                request.getServiceDate(),
                request.getAmount()
        );

        Claim savedClaim = claimRepository.save(claim);
        log.info("Claim created: id={}", savedClaim.getId());

        Receipt savedReceipt = receiptStorage.store(savedClaim, receipt);
        log.info("Receipt created: id={}", savedReceipt.getId());

        return new PostResponse(savedClaim.getId());
    }

    @Transactional(readOnly = true)
    public Resource getReceipt(UUID claimId) {
        Receipt receipt = receiptRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found with claim id: " + claimId));

        return new InputStreamResource(new ByteArrayInputStream(receipt.getContent()));
    }

    @Transactional
    public void approveClaim(UUID claimId) {
        Claim claim = claimRepository.findByIdOrThrow(claimId);

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimNotPendingException(
                    "Claim with id '" + claimId + "' cannot be approved because it is not pending. "
                            + "Current status: " + claim.getStatus()
            );
        }

        claim.setStatus(ClaimStatus.APPROVED);
        log.info("Claim approved: id={}", claimId);
    }

    private boolean hasAuthority(String authority) {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    @Transactional
    public void updateClaim(UUID claimId, UpdateClaimRequest request) {
        Claim claim = claimRepository.findByIdOrThrow(claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimUpdateNotAllowedException("Only pending claims can be updated");
        }

        claimMapper.updateFromUpdateRequest(claim, request);
        Enrollment currentEnrollment = claim.getEnrollment();
        if (!currentEnrollment.getPlan().getId().equals(request.getPlanId())) {
            Enrollment targetEnrollment = enrollmentRepository
                    .findByConsumerIdAndPlanId(currentEnrollment.getConsumer().getId(), request.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Enrollment not found for consumer and plan: " + request.getPlanId()));
            claim.setEnrollment(targetEnrollment);
        }
        claimRepository.save(claim);
    }
}
