package com.iam.platform.core.service;

import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.repository.NaturalPersonRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NaturalPersonService Tests")
class NaturalPersonServiceTest {

    @Mock
    private NaturalPersonRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NaturalPersonService service;

    private static final UUID TEST_ID = UUID.randomUUID();

    private NaturalPerson createTestPerson() {
        NaturalPerson person = new NaturalPerson();
        person.setId(TEST_ID);
        person.setPersonalIdCode("KH-123456789");
        person.setNationalIdNumber("012345678");
        person.setFirstNameKh("សុខ");
        person.setLastNameKh("ដារា");
        person.setFirstNameEn("Sok");
        person.setLastNameEn("Dara");
        person.setDateOfBirth(LocalDate.of(1990, 1, 15));
        person.setGender("MALE");
        person.setNationality("KH");
        person.setKeycloakUserId("kc-user-1");
        person.setStatus("ACTIVE");
        return person;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should save and return the natural person")
        void createSuccess() {
            NaturalPerson person = createTestPerson();
            when(repository.save(any(NaturalPerson.class))).thenReturn(person);

            NaturalPerson result = service.create(person);

            assertThat(result).isNotNull();
            assertThat(result.getPersonalIdCode()).isEqualTo("KH-123456789");
            verify(repository).save(person);
            verify(auditService).logApiAccess(eq("kc-user-1"), eq("CREATE_PERSON"),
                    contains("natural_persons/"), isNull(), eq(true));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should return person when found")
        void findByIdSuccess() {
            NaturalPerson person = createTestPerson();
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(person));

            NaturalPerson result = service.findById(TEST_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_ID);
            verify(repository).findById(TEST_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void findByIdNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("NaturalPerson")
                    .hasMessageContaining(TEST_ID.toString());
        }
    }

    @Nested
    @DisplayName("findByKeycloakUserId()")
    class FindByKeycloakUserId {

        @Test
        @DisplayName("Should return person when found by keycloak ID")
        void findByKeycloakUserIdSuccess() {
            NaturalPerson person = createTestPerson();
            when(repository.findByKeycloakUserId("kc-user-1")).thenReturn(Optional.of(person));

            NaturalPerson result = service.findByKeycloakUserId("kc-user-1");

            assertThat(result).isNotNull();
            assertThat(result.getKeycloakUserId()).isEqualTo("kc-user-1");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found by keycloak ID")
        void findByKeycloakUserIdNotFound() {
            when(repository.findByKeycloakUserId("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByKeycloakUserId("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByPersonalIdCode()")
    class FindByPersonalIdCode {

        @Test
        @DisplayName("Should return person when found by personal ID code")
        void findByPersonalIdCodeSuccess() {
            NaturalPerson person = createTestPerson();
            when(repository.findByPersonalIdCode("KH-123456789")).thenReturn(Optional.of(person));

            NaturalPerson result = service.findByPersonalIdCode("KH-123456789");

            assertThat(result.getPersonalIdCode()).isEqualTo("KH-123456789");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void findByPersonalIdCodeNotFound() {
            when(repository.findByPersonalIdCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByPersonalIdCode("INVALID"))
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
            NaturalPerson person = createTestPerson();
            Page<NaturalPerson> page = new PageImpl<>(List.of(person), pageable, 1);
            when(repository.findAll(pageable)).thenReturn(page);

            Page<NaturalPerson> result = service.findAll(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty page when no persons exist")
        void findAllEmpty() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NaturalPerson> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(repository.findAll(pageable)).thenReturn(emptyPage);

            Page<NaturalPerson> result = service.findAll(pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Should update and return the person")
        void updateSuccess() {
            NaturalPerson existing = createTestPerson();
            NaturalPerson updated = createTestPerson();
            updated.setFirstNameEn("UpdatedName");
            updated.setLastNameEn("UpdatedLast");

            when(repository.findById(TEST_ID)).thenReturn(Optional.of(existing));
            when(repository.save(any(NaturalPerson.class))).thenAnswer(inv -> inv.getArgument(0));

            NaturalPerson result = service.update(TEST_ID, updated);

            assertThat(result.getFirstNameEn()).isEqualTo("UpdatedName");
            assertThat(result.getLastNameEn()).isEqualTo("UpdatedLast");
            verify(repository).save(existing);
            verify(auditService).logApiAccess(isNull(), eq("UPDATE_PERSON"),
                    contains("natural_persons/"), isNull(), eq(true));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent person")
        void updateNotFound() {
            NaturalPerson updated = createTestPerson();
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TEST_ID, updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("softDelete()")
    class SoftDelete {

        @Test
        @DisplayName("Should soft delete the person")
        void softDeleteSuccess() {
            NaturalPerson person = createTestPerson();
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(person));
            when(repository.save(any(NaturalPerson.class))).thenReturn(person);

            service.softDelete(TEST_ID);

            assertThat(person.isDeleted()).isTrue();
            assertThat(person.getDeletedAt()).isNotNull();
            verify(repository).save(person);
            verify(auditService).logApiAccess(isNull(), eq("DELETE_PERSON"),
                    contains("natural_persons/"), isNull(), eq(true));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when soft-deleting non-existent person")
        void softDeleteNotFound() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.softDelete(TEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
