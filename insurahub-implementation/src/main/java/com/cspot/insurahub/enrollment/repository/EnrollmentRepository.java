package com.cspot.insurahub.enrollment.repository;

import com.cspot.insurahub.enrollment.entity.Enrollment;
import com.cspot.insurahub.insurancepackage.entity.InsurancePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {

    boolean existsByConsumerIdAndPlanId(UUID consumerId, UUID planId);

    Optional<Enrollment> findByConsumerIdAndPlanId(UUID consumerId, UUID planId);

    boolean existsByPlanInsurancePackage(InsurancePackage insurancePackage);
}
