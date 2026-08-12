package com.cspot.insurahub.claim.mapper;

import com.cspot.insurahub.claim.entity.Claim;
import com.cspot.insurahub.consumer.entity.Consumer;
import com.cspot.insurahub.model.ClaimResponse;
import com.cspot.insurahub.model.UpdateClaimRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class ClaimMapper {

    @Mapping(target = "consumerId", source = "claim.enrollment.consumer.id")
    @Mapping(target = "consumerFullName", source = "claim.enrollment.consumer", qualifiedByName = "fullName")
    @Mapping(target = "planId", source = "claim.enrollment.plan.id")
    @Mapping(target = "planName", source = "claim.enrollment.plan.name")
    @Mapping(target = "lastUpdateDate", expression = "java(toLastUpdateDate(claim))")
    public abstract ClaimResponse toListItemResponse(Claim claim);

    @Named("fullName")
    protected String consumerFullName(Consumer consumer) {
        return consumer.getFirstName() + " " + consumer.getLastName();
    }

    protected LocalDate toLastUpdateDate(Claim claim) {
        Instant lastUpdate = claim.getUpdatedAt() != null ? claim.getUpdatedAt() : claim.getCreatedAt();
        return lastUpdate.atZone(ZoneOffset.UTC).toLocalDate();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enrollment", ignore = true)
    @Mapping(target = "receipt", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract void updateFromUpdateRequest(@MappingTarget Claim claim, UpdateClaimRequest request);
}
