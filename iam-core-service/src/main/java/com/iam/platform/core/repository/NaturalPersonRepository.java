package com.iam.platform.core.repository;

import com.iam.platform.core.entity.NaturalPerson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NaturalPersonRepository extends JpaRepository<NaturalPerson, UUID> {

    Optional<NaturalPerson> findByPersonalIdCode(String personalIdCode);

    Optional<NaturalPerson> findByNationalIdNumber(String nationalIdNumber);

    Optional<NaturalPerson> findByCamDigiKeyId(String camDigiKeyId);

    Optional<NaturalPerson> findByKeycloakUserId(String keycloakUserId);

    Page<NaturalPerson> findByStatus(String status, Pageable pageable);
}
