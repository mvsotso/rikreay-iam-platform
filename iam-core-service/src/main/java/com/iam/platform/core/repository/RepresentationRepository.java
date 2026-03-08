package com.iam.platform.core.repository;

import com.iam.platform.core.entity.Representation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepresentationRepository extends JpaRepository<Representation, UUID> {

    Page<Representation> findByLegalEntityId(UUID legalEntityId, Pageable pageable);

    Page<Representation> findByNaturalPersonId(UUID naturalPersonId, Pageable pageable);

    List<Representation> findByLegalEntityIdAndRepresentativeRole(
            UUID legalEntityId,
            com.iam.platform.common.enums.RepresentativeRole role);
}
