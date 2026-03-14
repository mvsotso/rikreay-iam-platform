import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { NaturalPerson, LegalEntity, Representation, CreatePersonRequest, UpdatePersonRequest, CreateEntityRequest, UpdateEntityRequest, CreateRepresentationRequest } from '@/types/identity';

// Query keys
export const identityKeys = {
  persons: ['persons'] as const,
  personList: (params: PageParams & { search?: string }) => ['persons', 'list', params] as const,
  personDetail: (id: string) => ['persons', 'detail', id] as const,
  entities: ['entities'] as const,
  entityList: (params: PageParams & { search?: string; memberClass?: string }) => ['entities', 'list', params] as const,
  entityDetail: (id: string) => ['entities', 'detail', id] as const,
  representations: ['representations'] as const,
  representationList: (params: PageParams & { personId?: string; entityId?: string }) => ['representations', 'list', params] as const,
};

// --- Persons ---
export function usePersons(params: PageParams & { search?: string } = {}) {
  const { search, ...pageParams } = params;
  const queryStr = buildPageParams(pageParams) + (search ? `&search=${encodeURIComponent(search)}` : '');
  return useQuery({
    queryKey: identityKeys.personList(params),
    queryFn: () => api.get<Page<NaturalPerson>>(`/api/v1/persons?${queryStr}`),
  });
}

export function usePerson(id: string) {
  return useQuery({
    queryKey: identityKeys.personDetail(id),
    queryFn: () => api.get<NaturalPerson>(`/api/v1/persons/${id}`),
    enabled: !!id,
  });
}

export function useCreatePerson() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePersonRequest) => api.post<NaturalPerson>('/api/v1/persons', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: identityKeys.persons }); },
  });
}

export function useUpdatePerson(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdatePersonRequest) => api.put<NaturalPerson>(`/api/v1/persons/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: identityKeys.persons });
      queryClient.invalidateQueries({ queryKey: identityKeys.personDetail(id) });
    },
  });
}

export function useDeletePerson() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/api/v1/persons/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: identityKeys.persons }); },
  });
}

// --- Entities ---
export function useEntities(params: PageParams & { search?: string; memberClass?: string } = {}) {
  const { search, memberClass, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (search) queryStr += `&search=${encodeURIComponent(search)}`;
  if (memberClass) queryStr += `&memberClass=${memberClass}`;
  return useQuery({
    queryKey: identityKeys.entityList(params),
    queryFn: () => api.get<Page<LegalEntity>>(`/api/v1/entities?${queryStr}`),
  });
}

export function useEntity(id: string) {
  return useQuery({
    queryKey: identityKeys.entityDetail(id),
    queryFn: () => api.get<LegalEntity>(`/api/v1/entities/${id}`),
    enabled: !!id,
  });
}

export function useCreateEntity() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateEntityRequest) => api.post<LegalEntity>('/api/v1/entities', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: identityKeys.entities }); },
  });
}

export function useUpdateEntity(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateEntityRequest) => api.put<LegalEntity>(`/api/v1/entities/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: identityKeys.entities });
      queryClient.invalidateQueries({ queryKey: identityKeys.entityDetail(id) });
    },
  });
}

export function useDeleteEntity() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/api/v1/entities/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: identityKeys.entities }); },
  });
}

// --- Representations ---
export function useRepresentations(params: PageParams & { personId?: string; entityId?: string } = {}) {
  const { personId, entityId, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (personId) queryStr += `&personId=${personId}`;
  if (entityId) queryStr += `&entityId=${entityId}`;
  return useQuery({
    queryKey: identityKeys.representationList(params),
    queryFn: () => api.get<Page<Representation>>(`/api/v1/representations?${queryStr}`),
  });
}

export function useCreateRepresentation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateRepresentationRequest) => api.post<Representation>('/api/v1/representations', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: identityKeys.representations }); },
  });
}

export function useDeleteRepresentation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/api/v1/representations/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: identityKeys.representations }); },
  });
}

// --- My Profile ---
export function useMyProfile() {
  return useQuery({
    queryKey: ['my-profile'],
    queryFn: () => api.get<NaturalPerson>('/api/v1/persons/me'),
  });
}
