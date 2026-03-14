package com.iam.platform.core.service;

import com.iam.platform.common.enums.DelegationScope;
import com.iam.platform.common.enums.RepresentativeRole;
import com.iam.platform.common.exception.IamPlatformException;
import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.repository.RepresentationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepresentationService Tests")
class RepresentationServiceTest {

    @Mock
    private RepresentationRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RepresentationService service;

    private static final UUID TEST_ID = UUID.randomUUID();
    private static final UUID TEST_ENTITY_ID = UUID.randomUUID();
    private static final UUID TEST_PERSON_ID = UUID.randomUUID();

    private Representation createTestRepresentation() {
        LegalEntity entity = new LegalEntity();
        entity.setId(TEST_ENTITY_ID);

        NaturalPerson person = new NaturalPerson();
        person.setId(TEST_PERSON_ID);

        Representation rep = new Representation();
        rep.setId(TEST_ID);
        rep.setNaturalPerson(person);
        rep.setLegalEntity(entity);
        rep.setRepresentativeRole(RepresentativeRole.LEGAL_REPRESENTATIVE);
        rep.setDelegationScope(DelegationScope.FULL_AUTHORITY);
        rep.setTitle("CEO");
        rep.setValidFrom(LocalDate.of(2024, 1, 1));
        rep.setValidUntil(LocalDate.of(2025, 12, 31));
        rep.setIsPrimary(true);
        rep.setStatus("ACTIVE");
        return rep;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should save and return the representation")
        void createSuccess() {
            Representation rep = createTestRepresentation();
            when(repository.save(any(Representation.class))).thenReturn(rep);

            Representation result = service.create(rep);

            assertThat(result).isNotNull();
            assertThat(result.getRepresentativeRole()).isEqualTo(RepresentativeRole.LEGAL_REPRESENTATIVE);
            verify(repository).save(rep);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should return representation when found")
        void findByIdSuccess() {
            Representation rep = createTestRepresentation();
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(rep));

            Representation result = service.findById(TEST_ID);

            assertThat(result.getId()).isEqualTo(TEST_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void findByIdNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Representation");
        }
    }

    @Nested
    @DisplayName("findByLegalEntity()")
    class FindByLegalEntity {

        @Test
        @DisplayName("Should return paginated results by legal entity")
        void findByLegalEntitySuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Representation> page = new PageImpl<>(List.of(createTestRepresentation()), pageable, 1);
            when(repository.findByLegalEntityId(TEST_ENTITY_ID, pageable)).thenReturn(page);

            Page<Representation> result = service.findByLegalEntity(TEST_ENTITY_ID, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findByNaturalPerson()")
    class FindByNaturalPerson {

        @Test
        @DisplayName("Should return paginated results by natural person")
        void findByNaturalPersonSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Representation> page = new PageImpl<>(List.of(createTestRepresentation()), pageable, 1);
            when(repository.findByNaturalPersonId(TEST_PERSON_ID, pageable)).thenReturn(page);

            Page<Representation> result = service.findByNaturalPerson(TEST_PERSON_ID, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Should update and return representation")
        void updateSuccess() {
            Representation existing = createTestRepresentation();
            Representation updated = createTestRepresentation();
            updated.setRepresentativeRole(RepresentativeRole.FINANCE_OFFICER);
            updated.setDelegationScope(DelegationScope.LIMITED);
            updated.setTitle("CFO");

            when(repository.findById(TEST_ID)).thenReturn(Optional.of(existing));
            when(repository.save(any(Representation.class))).thenAnswer(inv -> inv.getArgument(0));

            Representation result = service.update(TEST_ID, updated);

            assertThat(result.getRepresentativeRole()).isEqualTo(RepresentativeRole.FINANCE_OFFICER);
            assertThat(result.getDelegationScope()).isEqualTo(DelegationScope.LIMITED);
            assertThat(result.getTitle()).isEqualTo("CFO");
            verify(repository).save(existing);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent representation")
        void updateNotFound() {
            Representation updated = createTestRepresentation();
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TEST_ID, updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("revoke()")
    class Revoke {

        @Test
        @DisplayName("Should soft delete the representation")
        void revokeSuccess() {
            Representation rep = createTestRepresentation();
            rep.setRepresentativeRole(RepresentativeRole.FINANCE_OFFICER); // Not last legal rep

            when(repository.findById(TEST_ID)).thenReturn(Optional.of(rep));
            when(repository.save(any(Representation.class))).thenReturn(rep);

            service.revoke(TEST_ID);

            assertThat(rep.isDeleted()).isTrue();
            verify(repository).save(rep);
        }

        @Test
        @DisplayName("Should allow revoking LEGAL_REPRESENTATIVE when others exist")
        void revokeLegalRepWithOthersExisting() {
            Representation rep = createTestRepresentation();
            Representation otherRep = createTestRepresentation();
            otherRep.setId(UUID.randomUUID());

            when(repository.findById(TEST_ID)).thenReturn(Optional.of(rep));
            when(repository.findByLegalEntityIdAndRepresentativeRole(
                    TEST_ENTITY_ID, RepresentativeRole.LEGAL_REPRESENTATIVE))
                    .thenReturn(List.of(rep, otherRep));
            when(repository.save(any(Representation.class))).thenReturn(rep);

            service.revoke(TEST_ID);

            assertThat(rep.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when revoking last LEGAL_REPRESENTATIVE")
        void revokeLastLegalRepresentative() {
            Representation rep = createTestRepresentation();

            when(repository.findById(TEST_ID)).thenReturn(Optional.of(rep));
            when(repository.findByLegalEntityIdAndRepresentativeRole(
                    TEST_ENTITY_ID, RepresentativeRole.LEGAL_REPRESENTATIVE))
                    .thenReturn(List.of(rep));

            assertThatThrownBy(() -> service.revoke(TEST_ID))
                    .isInstanceOf(IamPlatformException.class)
                    .hasMessageContaining("Cannot revoke the last LEGAL_REPRESENTATIVE");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when revoking non-existent representation")
        void revokeNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revoke(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
