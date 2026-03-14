'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateEntity } from '@/services/identity';
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
  registrationNumber: z.string().min(1, 'Registration number is required'),
  tin: z.string().optional(),
  nameKh: z.string().min(1, 'Khmer name is required'),
  nameEn: z.string().optional(),
  entityType: z.enum(ENTITY_TYPES),
  memberClass: z.enum(['GOV', 'COM', 'NGO', 'MUN']),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  phone: z.string().optional(),
  website: z.string().url('Invalid URL').optional().or(z.literal('')),
});

type EntityFormValues = z.infer<typeof entitySchema>;

export default function NewEntityPage() {
  const router = useRouter();
  const createEntity = useCreateEntity();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<EntityFormValues>({
    resolver: zodResolver(entitySchema),
    defaultValues: { memberClass: 'COM', entityType: 'PRIVATE_LLC' },
  });

  const onSubmit = async (data: EntityFormValues) => {
    try {
      const cleaned = { ...data, email: data.email || undefined, website: data.website || undefined };
      await createEntity.mutateAsync(cleaned);
      toast.success('Entity created');
      router.push('/identity/entities');
    } catch {
      toast.error('Failed to create entity');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="New Legal Entity" description="Register a new legal entity" actions={<Link href="/identity/entities" className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"><ArrowLeft className="h-4 w-4" /> Back</Link>} />

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
              {errors.nameKh && <p className="mt-1 text-xs text-destructive">{errors.nameKh.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Name (English)</label>
              <input {...register('nameEn')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium">Member Class *</label>
              <select {...register('memberClass')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                <option value="GOV">Government (GOV)</option>
                <option value="COM">Commercial (COM)</option>
                <option value="NGO">NGO</option>
                <option value="MUN">Municipal (MUN)</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium">Entity Type *</label>
              <select {...register('entityType')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                {ENTITY_TYPES.map((t) => (
                  <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Contact</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Email</label>
              <input {...register('email')} type="email" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.email && <p className="mt-1 text-xs text-destructive">{errors.email.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Phone</label>
              <input {...register('phone')} type="tel" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div className="col-span-2">
              <label className="text-sm font-medium">Website</label>
              <input {...register('website')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="https://..." />
              {errors.website && <p className="mt-1 text-xs text-destructive">{errors.website.message}</p>}
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
            {isSubmitting ? 'Creating...' : 'Create Entity'}
          </button>
          <Link href="/identity/entities" className="rounded-md border px-6 py-2 text-sm hover:bg-accent">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
