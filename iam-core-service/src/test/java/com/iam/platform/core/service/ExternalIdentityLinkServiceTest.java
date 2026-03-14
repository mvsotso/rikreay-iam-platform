package com.iam.platform.core.service;

import com.iam.platform.common.enums.ExternalSystem;
import com.iam.platform.common.enums.VerificationStatus;
import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.ExternalIdentityLink;
import com.iam.platform.core.repository.ExternalIdentityLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalIdentityLinkService Tests")
class ExternalIdentityLinkServiceTest {

    @Mock
    private ExternalIdentityLinkRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ExternalIdentityLinkService service;

    private static final UUID TEST_ID = UUID.randomUUID();
    private static final UUID TEST_OWNER_ID = UUID.randomUUID();

    private ExternalIdentityLink createTestLink() {
        ExternalIdentityLink link = new ExternalIdentityLink();
        link.setId(TEST_ID);
        link.setOwnerType("NATURAL_PERSON");
        link.setOwnerId(TEST_OWNER_ID);
        link.setExternalSystem(ExternalSystem.MOI_NATIONAL_ID);
        link.setExternalIdentifier("NID-123456");
        link.setVerificationStatus(VerificationStatus.UNVERIFIED);
        return link;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should save and return the external link")
        void createSuccess() {
            ExternalIdentityLink link = createTestLink();
            when(repository.save(any(ExternalIdentityLink.class))).thenReturn(link);

            ExternalIdentityLink result = service.create(link);

            assertThat(result).isNotNull();
            assertThat(result.getExternalSystem()).isEqualTo(ExternalSystem.MOI_NATIONAL_ID);
            verify(repository).save(link);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should return link when found")
        void findByIdSuccess() {
            ExternalIdentityLink link = createTestLink();
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(link));

            ExternalIdentityLink result = service.findById(TEST_ID);

            assertThat(result.getId()).isEqualTo(TEST_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void findByIdNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ExternalIdentityLink");
        }
    }

    @Nested
    @DisplayName("findByOwner()")
    class FindByOwner {

        @Test
        @DisplayName("Should return links by owner")
        void findByOwnerSuccess() {
            ExternalIdentityLink link = createTestLink();
            when(repository.findByOwnerTypeAndOwnerId("NATURAL_PERSON", TEST_OWNER_ID))
                    .thenReturn(List.of(link));

            List<ExternalIdentityLink> result = service.findByOwner("NATURAL_PERSON", TEST_OWNER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getExternalIdentifier()).isEqualTo("NID-123456");
        }

        @Test
        @DisplayName("Should return empty list when no links exist")
        void findByOwnerEmpty() {
            when(repository.findByOwnerTypeAndOwnerId("NATURAL_PERSON", TEST_OWNER_ID))
                    .thenReturn(List.of());

            List<ExternalIdentityLink> result = service.findByOwner("NATURAL_PERSON", TEST_OWNER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBySystemAndIdentifier()")
    class FindBySystemAndIdentifier {

        @Test
        @DisplayName("Should return link when found by system and identifier")
        void findBySystemAndIdentifierSuccess() {
            ExternalIdentityLink link = createTestLink();
            when(repository.findByExternalSystemAndExternalIdentifier(
                    ExternalSystem.MOI_NATIONAL_ID, "NID-123456"))
                    .thenReturn(Optional.of(link));

            Optional<ExternalIdentityLink> result = service.findBySystemAndIdentifier(
                    ExternalSystem.MOI_NATIONAL_ID, "NID-123456");

            assertThat(result).isPresent();
            assertThat(result.get().getExternalIdentifier()).isEqualTo("NID-123456");
        }

        @Test
        @DisplayName("Should return empty when not found")
        void findBySystemAndIdentifierNotFound() {
            when(repository.findByExternalSystemAndExternalIdentifier(
                    ExternalSystem.CAMDIGIKEY, "INVALID"))
                    .thenReturn(Optional.empty());

            Optional<ExternalIdentityLink> result = service.findBySystemAndIdentifier(
                    ExternalSystem.CAMDIGIKEY, "INVALID");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Should update verification fields")
        void updateSuccess() {
            ExternalIdentityLink existing = createTestLink();
            ExternalIdentityLink updated = createTestLink();
            updated.setVerificationStatus(VerificationStatus.VERIFIED);
            updated.setVerifiedAt(Instant.now());
            updated.setVerificationMethod("API_LOOKUP");

            when(repository.findById(TEST_ID)).thenReturn(Optional.of(existing));
            when(repository.save(any(ExternalIdentityLink.class))).thenAnswer(inv -> inv.getArgument(0));

            ExternalIdentityLink result = service.update(TEST_ID, updated);

            assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(result.getVerificationMethod()).isEqualTo("API_LOOKUP");
            verify(repository).save(existing);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent link")
        void updateNotFound() {
            ExternalIdentityLink updated = createTestLink();
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TEST_ID, updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("softDelete()")
    class SoftDelete {

        @Test
        @DisplayName("Should soft delete the link")
        void softDeleteSuccess() {
            ExternalIdentityLink link = createTestLink();
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(link));
            when(repository.save(any(ExternalIdentityLink.class))).thenReturn(link);

            service.softDelete(TEST_ID);

            assertThat(link.isDeleted()).isTrue();
            assertThat(link.getDeletedAt()).isNotNull();
            verify(repository).save(link);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when soft-deleting non-existent link")
        void softDeleteNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.softDelete(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
