'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateTenant } from '@/services/admin';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const tenantSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  memberClass: z.enum(['GOV', 'COM', 'NGO', 'MUN']),
  description: z.string().optional(),
  adminEmail: z.string().email('Valid email required'),
  adminUsername: z.string().min(3, 'Min 3 characters'),
  adminFirstName: z.string().min(1, 'Required'),
  adminLastName: z.string().min(1, 'Required'),
  maxUsers: z.coerce.number().min(1, 'At least 1 user').max(10000),
});

type TenantFormValues = z.infer<typeof tenantSchema>;

export default function NewTenantPage() {
  const router = useRouter();
  const createTenant = useCreateTenant();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<TenantFormValues>({
    resolver: zodResolver(tenantSchema),
    defaultValues: { memberClass: 'COM', maxUsers: 100 },
  });

  const onSubmit = async (data: TenantFormValues) => {
    try {
      await createTenant.mutateAsync(data);
      toast.success('Tenant created with Keycloak realm');
      router.push('/tenants');
    } catch {
      toast.error('Failed to create tenant');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="New Tenant" description="Provision a new organization with Keycloak realm" actions={<Link href="/tenants" className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"><ArrowLeft className="h-4 w-4" /> Back</Link>} />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Organization</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Name *</label>
              <input {...register('name')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.name && <p className="mt-1 text-xs text-destructive">{errors.name.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Sector *</label>
              <select {...register('memberClass')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                <option value="GOV">Government (GOV)</option>
                <option value="COM">Commercial (COM)</option>
                <option value="NGO">NGO</option>
                <option value="MUN">Municipal (MUN)</option>
              </select>
            </div>
            <div className="col-span-2">
              <label className="text-sm font-medium">Description</label>
              <textarea {...register('description')} rows={2} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium">Max Users *</label>
              <input {...register('maxUsers')} type="number" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.maxUsers && <p className="mt-1 text-xs text-destructive">{errors.maxUsers.message}</p>}
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Initial Admin User</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Username *</label>
              <input {...register('adminUsername')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.adminUsername && <p className="mt-1 text-xs text-destructive">{errors.adminUsername.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Email *</label>
              <input {...register('adminEmail')} type="email" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.adminEmail && <p className="mt-1 text-xs text-destructive">{errors.adminEmail.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">First Name *</label>
              <input {...register('adminFirstName')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.adminFirstName && <p className="mt-1 text-xs text-destructive">{errors.adminFirstName.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Last Name *</label>
              <input {...register('adminLastName')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.adminLastName && <p className="mt-1 text-xs text-destructive">{errors.adminLastName.message}</p>}
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">{isSubmitting ? 'Provisioning...' : 'Create Tenant'}</button>
          <Link href="/tenants" className="rounded-md border px-6 py-2 text-sm hover:bg-accent">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
