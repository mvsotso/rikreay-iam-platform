package com.iam.platform.core.service;

import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.repository.LegalEntityRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegalEntityService Tests")
class LegalEntityServiceTest {

    @Mock
    private LegalEntityRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LegalEntityService service;

    private static final UUID TEST_ID = UUID.randomUUID();

    private LegalEntity createTestEntity() {
        LegalEntity entity = new LegalEntity();
        entity.setId(TEST_ID);
        entity.setRegistrationNumber("REG-001");
        entity.setTaxIdentificationNumber("TIN-001");
        entity.setNameKh("ក្រុមហ៊ុន សាកល្បង");
        entity.setNameEn("Test Company LLC");
        entity.setEntityType(EntityType.PRIVATE_LLC);
        entity.setMemberClass(MemberClass.COM);
        entity.setXroadMemberCode("COM/test-company");
        entity.setRealmName("test-company");
        entity.setProvince("Phnom Penh");
        entity.setStatus("ACTIVE");
        return entity;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should set status to ACTIVE and save")
        void createSuccess() {
            LegalEntity entity = createTestEntity();
            entity.setStatus(null); // Should be set by service
            when(repository.save(any(LegalEntity.class))).thenReturn(entity);

            LegalEntity result = service.create(entity);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(repository).save(entity);
            verify(auditService).logApiAccess(isNull(), eq("CREATE_ENTITY"),
                    contains("legal_entities/"), isNull(), eq(true));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should return entity when found")
        void findByIdSuccess() {
            LegalEntity entity = createTestEntity();
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(entity));

            LegalEntity result = service.findById(TEST_ID);

            assertThat(result.getId()).isEqualTo(TEST_ID);
            assertThat(result.getNameEn()).isEqualTo("Test Company LLC");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void findByIdNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LegalEntity");
        }
    }

    @Nested
    @DisplayName("findByRegistrationNumber()")
    class FindByRegistrationNumber {

        @Test
        @DisplayName("Should return entity when found by registration number")
        void success() {
            LegalEntity entity = createTestEntity();
            when(repository.findByRegistrationNumber("REG-001")).thenReturn(Optional.of(entity));

            LegalEntity result = service.findByRegistrationNumber("REG-001");

            assertThat(result.getRegistrationNumber()).isEqualTo("REG-001");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void notFound() {
            when(repository.findByRegistrationNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByRegistrationNumber("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByTin()")
    class FindByTin {

        @Test
        @DisplayName("Should return entity when found by TIN")
        void success() {
            LegalEntity entity = createTestEntity();
            when(repository.findByTaxIdentificationNumber("TIN-001")).thenReturn(Optional.of(entity));

            LegalEntity result = service.findByTin("TIN-001");

            assertThat(result.getTaxIdentificationNumber()).isEqualTo("TIN-001");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void notFound() {
            when(repository.findByTaxIdentificationNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByTin("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("Should return paginated results")
        void findAllSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            LegalEntity entity = createTestEntity();
            Page<LegalEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
            when(repository.findAll(pageable)).thenReturn(page);

            Page<LegalEntity> result = service.findAll(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Should update and return entity")
        void updateSuccess() {
            LegalEntity existing = createTestEntity();
            LegalEntity updated = createTestEntity();
            updated.setNameEn("Updated Company");
            updated.setProvince("Siem Reap");

            when(repository.findById(TEST_ID)).thenReturn(Optional.of(existing));
            when(repository.save(any(LegalEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LegalEntity result = service.update(TEST_ID, updated);

            assertThat(result.getNameEn()).isEqualTo("Updated Company");
            assertThat(result.getProvince()).isEqualTo("Siem Reap");
            verify(repository).save(existing);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent entity")
        void updateNotFound() {
            LegalEntity updated = createTestEntity();
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TEST_ID, updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("softDelete()")
    class SoftDelete {

        @Test
        @DisplayName("Should soft delete the entity")
        void softDeleteSuccess() {
            LegalEntity entity = createTestEntity();
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(entity));
            when(repository.save(any(LegalEntity.class))).thenReturn(entity);

            service.softDelete(TEST_ID);

            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedAt()).isNotNull();
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when soft-deleting non-existent entity")
        void softDeleteNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.softDelete(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
