import { MemberClass, EntityType, RepresentativeRole, DelegationScope, VerificationStatus, ChannelType } from './enums';

export interface NaturalPerson {
  id: string;
  personalIdCode: string;
  nationalIdNumber?: string;
  camDigiKeyId?: string;
  firstNameKh: string;
  lastNameKh: string;
  firstNameEn?: string;
  lastNameEn?: string;
  dateOfBirth?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  email?: string;
  phone?: string;
  verificationStatus: VerificationStatus;
  verificationLevel: number;
  keycloakUserId?: string;
  addresses?: Address[];
  contacts?: ContactChannel[];
  documents?: IdentityDocument[];
  createdAt: string;
  updatedAt: string;
}

export interface LegalEntity {
  id: string;
  registrationNumber: string;
  tin?: string;
  nameKh: string;
  nameEn?: string;
  entityType: EntityType;
  memberClass: MemberClass;
  realmName?: string;
  email?: string;
  phone?: string;
  website?: string;
  verificationStatus: VerificationStatus;
  addresses?: Address[];
  contacts?: ContactChannel[];
  representatives?: Representation[];
  createdAt: string;
  updatedAt: string;
}

export interface Representation {
  id: string;
  naturalPersonId: string;
  naturalPersonName?: string;
  legalEntityId: string;
  legalEntityName?: string;
  representativeRole: RepresentativeRole;
  delegationScope: DelegationScope;
  validFrom: string;
  validUntil?: string;
  authorizationDocument?: string;
  verificationStatus: VerificationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Address {
  id: string;
  addressType: 'HOME' | 'OFFICE' | 'REGISTERED';
  streetAddress?: string;
  sangkat?: string;
  khan?: string;
  province?: string;
  postalCode?: string;
  country: string;
}

export interface ContactChannel {
  id: string;
  channelType: ChannelType;
  value: string;
  verified: boolean;
  primary: boolean;
}

export interface IdentityDocument {
  id: string;
  documentType: string;
  documentNumber: string;
  issuedBy?: string;
  issuedDate?: string;
  expiryDate?: string;
  fileUrl?: string;
  fileHash?: string;
  verified: boolean;
}

export interface ExternalIdentityLink {
  id: string;
  externalSystem: string;
  externalId: string;
  linkedAt: string;
  verified: boolean;
}

// Form types for create/edit
export interface CreatePersonRequest {
  personalIdCode: string;
  nationalIdNumber?: string;
  firstNameKh: string;
  lastNameKh: string;
  firstNameEn?: string;
  lastNameEn?: string;
  dateOfBirth?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  email?: string;
  phone?: string;
}

export type UpdatePersonRequest = Partial<CreatePersonRequest>;

export interface CreateEntityRequest {
  registrationNumber: string;
  tin?: string;
  nameKh: string;
  nameEn?: string;
  entityType: EntityType;
  memberClass: MemberClass;
  email?: string;
  phone?: string;
  website?: string;
}

export type UpdateEntityRequest = Partial<CreateEntityRequest>;

export interface CreateRepresentationRequest {
  naturalPersonId: string;
  legalEntityId: string;
  representativeRole: RepresentativeRole;
  delegationScope: DelegationScope;
  validFrom: string;
  validUntil?: string;
  authorizationDocument?: string;
}
