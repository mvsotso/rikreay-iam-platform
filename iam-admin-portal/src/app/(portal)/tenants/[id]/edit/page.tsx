'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useTenant, useUpdateTenant } from '@/services/admin';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const schema = z.object({
  name: z.string().min(1),
  memberClass: z.enum(['GOV', 'COM', 'NGO', 'MUN']),
  description: z.string().optional(),
  maxUsers: z.coerce.number().min(1).max(10000),
});

type FormValues = z.infer<typeof schema>;

export default function EditTenantPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: tenant, isLoading } = useTenant(id);
  const updateTenant = useUpdateTenant(id);
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (tenant) reset({ name: tenant.name, memberClass: tenant.memberClass, description: tenant.description ?? '', maxUsers: tenant.maxUsers });
  }, [tenant, reset]);

  const onSubmit = async (data: FormValues) => {
    try {
      await updateTenant.mutateAsync(data);
      toast.success('Tenant updated');
      router.push(`/tenants/${id}`);
    } catch {
      toast.error('Failed to update tenant');
    }
  };

  if (isLoading) return <div className="h-96 rounded-lg bg-muted animate-pulse" />;

  return (
    <div className="space-y-6">
      <PageHeader title={`Edit: ${tenant?.name ?? ''}`} description="Update tenant settings" actions={<Link href={`/tenants/${id}`} className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"><ArrowLeft className="h-4 w-4" /> Back</Link>} />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Name *</label>
              <input {...register('name')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.name && <p className="mt-1 text-xs text-destructive">{errors.name.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Sector</label>
              <select {...register('memberClass')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                <option value="GOV">GOV</option><option value="COM">COM</option><option value="NGO">NGO</option><option value="MUN">MUN</option>
              </select>
            </div>
            <div className="col-span-2">
              <label className="text-sm font-medium">Description</label>
              <textarea {...register('description')} rows={2} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium">Max Users</label>
              <input {...register('maxUsers')} type="number" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">{isSubmitting ? 'Saving...' : 'Save Changes'}</button>
          <Link href={`/tenants/${id}`} className="rounded-md border px-6 py-2 text-sm hover:bg-accent">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
