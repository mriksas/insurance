package com.cspot.insurahub.claim.controller;

import com.cspot.insurahub.api.ClaimsApi;
import com.cspot.insurahub.claim.service.ClaimService;
import com.cspot.insurahub.model.ClaimResponse;
import com.cspot.insurahub.model.PostClaimRequest;
import com.cspot.insurahub.model.PostResponse;
import com.cspot.insurahub.model.UpdateClaimRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClaimController implements ClaimsApi {

    private final ClaimService claimService;

    @Override
    @PreAuthorize("hasAuthority('view:claims') || hasAuthority('view:own:claims')")
    public Page<ClaimResponse> getClaims(String claimNumber, String consumer, Pageable pageable) {
        return claimService.getClaims(claimNumber, consumer, pageable);
    }

    @Override
    @PreAuthorize("hasAuthority('create:claims')")
    public Resource getReceipt(UUID claimId) {
        return claimService.getReceipt(claimId);
    }

    @Override
    @PreAuthorize("hasAuthority('create:claims')")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse postClaim(
            @Valid @RequestPart("postClaimRequest") PostClaimRequest postClaimRequest,
            @RequestPart("receiptFile") MultipartFile receiptFile
    ) {
        return claimService.createClaim(postClaimRequest, receiptFile);
    }

    @Override
    @PreAuthorize("hasAuthority('update:claims')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveClaim(UUID claimId) {
        claimService.approveClaim(claimId);
    }

    @Override
    @PreAuthorize("hasAuthority('update:claims')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putClaim(UUID claimId, @Valid UpdateClaimRequest updateClaimRequest) {
        claimService.updateClaim(claimId, updateClaimRequest);
    }
}
