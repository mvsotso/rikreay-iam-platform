'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useEntity, useUpdateEntity } from '@/services/identity';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const ENTITY_TYPES = [
  'GOVERNMENT_MINISTRY', 'GOVERNMENT_DEPARTMENT', 'STATE_ENTERPRISE',
  'MUNICIPALITY', 'COMMUNE',
  'PRIVATE_LLC', 'SINGLE_MEMBER_LLC', 'PUBLIC_LIMITED', 'BRANCH_OFFICE', 'REPRESENTATIVE_OFFICE', 'SOLE_PROPRIETOR', 'PARTNERSHIP',
  'LOCAL_NGO', 'INTERNATIONAL_NGO', 'ASSOCIATION', 'FOREIGN_MISSION',
] as const;

const entitySchema = z.object({
  registrationNumber: z.string().min(1),
  tin: z.string().optional(),
  nameKh: z.string().min(1),
  nameEn: z.string().optional(),
  entityType: z.enum(ENTITY_TYPES),
  memberClass: z.enum(['GOV', 'COM', 'NGO', 'MUN']),
  email: z.string().email().optional().or(z.literal('')),
  phone: z.string().optional(),
  website: z.string().url().optional().or(z.literal('')),
});

type EntityFormValues = z.infer<typeof entitySchema>;

export default function EditEntityPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: entity, isLoading } = useEntity(id);
  const updateEntity = useUpdateEntity(id);
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<EntityFormValues>({
    resolver: zodResolver(entitySchema),
  });

  useEffect(() => {
    if (entity) {
      reset({
        registrationNumber: entity.registrationNumber,
        tin: entity.tin ?? '',
        nameKh: entity.nameKh,
        nameEn: entity.nameEn ?? '',
        entityType: entity.entityType,
        memberClass: entity.memberClass,
        email: entity.email ?? '',
        phone: entity.phone ?? '',
        website: entity.website ?? '',
      });
    }
  }, [entity, reset]);

  const onSubmit = async (data: EntityFormValues) => {
    try {
      await updateEntity.mutateAsync({ ...data, email: data.email || undefined, website: data.website || undefined });
      toast.success('Entity updated');
      router.push(`/identity/entities/${id}`);
    } catch {
      toast.error('Failed to update entity');
    }
  };

  if (isLoading) return <div className="h-96 rounded-lg bg-muted animate-pulse" />;

  return (
    <div className="space-y-6">
      <PageHeader title={`Edit: ${entity?.nameKh ?? ''}`} description="Update entity information" actions={<Link href={`/identity/entities/${id}`} className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"><ArrowLeft className="h-4 w-4" /> Back</Link>} />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Registration</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Registration Number *</label>
              <input {...register('registrationNumber')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.registrationNumber && <p className="mt-1 text-xs text-destructive">{errors.registrationNumber.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">TIN</label>
              <input {...register('tin')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Entity Details</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Name (Khmer) *</label>
              <input {...register('nameKh')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium">Name (English)</label>
              <input {...register('nameEn')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium">Member Class</label>
              <select {...register('memberClass')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                <option value="GOV">GOV</option><option value="COM">COM</option><option value="NGO">NGO</option><option value="MUN">MUN</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium">Entity Type</label>
              <select {...register('entityType')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                {ENTITY_TYPES.map((t) => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
              </select>
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Contact</h3>
          <div className="grid grid-cols-2 gap-4">
            <div><label className="text-sm font-medium">Email</label><input {...register('email')} type="email" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" /></div>
            <div><label className="text-sm font-medium">Phone</label><input {...register('phone')} type="tel" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" /></div>
            <div className="col-span-2"><label className="text-sm font-medium">Website</label><input {...register('website')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" /></div>
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">{isSubmitting ? 'Saving...' : 'Save Changes'}</button>
          <Link href={`/identity/entities/${id}`} className="rounded-md border px-6 py-2 text-sm hover:bg-accent">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
