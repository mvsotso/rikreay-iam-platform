'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateRepresentation } from '@/services/identity';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const REPRESENTATIVE_ROLES = ['LEGAL_REPRESENTATIVE', 'AUTHORIZED_SIGNATORY', 'TAX_REPRESENTATIVE', 'FINANCE_OFFICER', 'IT_ADMINISTRATOR', 'COMPLIANCE_OFFICER', 'GOVERNMENT_OFFICER', 'DELEGATED_USER', 'EXTERNAL_AUDITOR'] as const;
const DELEGATION_SCOPES = ['FULL', 'LIMITED', 'READ_ONLY', 'SPECIFIC'] as const;

const schema = z.object({
  naturalPersonId: z.string().min(1, 'Person ID is required'),
  legalEntityId: z.string().min(1, 'Entity ID is required'),
  representativeRole: z.enum(REPRESENTATIVE_ROLES),
  delegationScope: z.enum(DELEGATION_SCOPES),
  validFrom: z.string().min(1, 'Valid from is required'),
  validUntil: z.string().optional(),
  authorizationDocument: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export default function NewRepresentationPage() {
  const router = useRouter();
  const createRep = useCreateRepresentation();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { representativeRole: 'DELEGATED_USER', delegationScope: 'READ_ONLY' },
  });

  const onSubmit = async (data: FormValues) => {
    try {
      await createRep.mutateAsync(data);
      toast.success('Representation created');
      router.push('/identity/representations');
    } catch {
      toast.error('Failed to create representation');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="New Representation" description="Create a person-to-entity representation" actions={<Link href="/identity/representations" className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"><ArrowLeft className="h-4 w-4" /> Back</Link>} />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Parties</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Natural Person ID *</label>
              <input {...register('naturalPersonId')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="UUID of the person" />
              {errors.naturalPersonId && <p className="mt-1 text-xs text-destructive">{errors.naturalPersonId.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Legal Entity ID *</label>
              <input {...register('legalEntityId')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="UUID of the entity" />
              {errors.legalEntityId && <p className="mt-1 text-xs text-destructive">{errors.legalEntityId.message}</p>}
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Delegation</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Role *</label>
              <select {...register('representativeRole')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                {REPRESENTATIVE_ROLES.map((r) => <option key={r} value={r}>{r.replace(/_/g, ' ')}</option>)}
              </select>
            </div>
            <div>
              <label className="text-sm font-medium">Scope *</label>
              <select {...register('delegationScope')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                {DELEGATION_SCOPES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div>
              <label className="text-sm font-medium">Valid From *</label>
              <input {...register('validFrom')} type="date" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.validFrom && <p className="mt-1 text-xs text-destructive">{errors.validFrom.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Valid Until</label>
              <input {...register('validUntil')} type="date" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
          </div>
          <div>
            <label className="text-sm font-medium">Authorization Document Reference</label>
            <input {...register('authorizationDocument')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="Document reference or ID" />
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">{isSubmitting ? 'Creating...' : 'Create Representation'}</button>
          <Link href="/identity/representations" className="rounded-md border px-6 py-2 text-sm hover:bg-accent">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
