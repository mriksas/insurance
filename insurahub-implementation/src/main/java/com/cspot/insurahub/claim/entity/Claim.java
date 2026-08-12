package com.cspot.insurahub.claim.entity;

import com.cspot.insurahub.claim.enumeration.ClaimStatus;
import com.cspot.insurahub.common.SoftDeletableAuditableEntity;
import com.cspot.insurahub.enrollment.entity.Enrollment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "claims")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Claim extends SoftDeletableAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    @Setter
    private Enrollment enrollment;

    @OneToOne(mappedBy = "claim", fetch = FetchType.LAZY)
    @Setter(AccessLevel.PACKAGE)
    private Receipt receipt;

    @Column(name = "service_date", nullable = false)
    @Setter
    private LocalDate serviceDate;

    @Column(
            name = "claim_number",
            nullable = false,
            unique = true,
            length = 13,
            insertable = false,
            updatable = false
    )
    @Setter
    private String claimNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    @Setter
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Setter
    private ClaimStatus status;

    public Claim(
            Enrollment enrollment,
            LocalDate serviceDate,
            BigDecimal amount
    ) {
        this.enrollment = enrollment;
        this.serviceDate = serviceDate;
        this.amount = amount;
        this.status = ClaimStatus.PENDING;

        if (enrollment != null) {
            enrollment.getClaims().add(this);
        }
    }

}
