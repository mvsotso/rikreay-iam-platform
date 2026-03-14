'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreatePolicy } from '@/services/governance';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const policySchema = z.object({
  name: z.string().min(1, 'Name is required').max(200, 'Name must be under 200 characters'),
  description: z.string().optional(),
  policyType: z.enum(['ACCESS', 'SEGREGATION_OF_DUTIES', 'RECERTIFICATION', 'RISK_BASED'], {
    required_error: 'Policy type is required',
  }),
  enabled: z.boolean().default(true),
});

type PolicyFormValues = z.infer<typeof policySchema>;

export default function NewPolicyPage() {
  const router = useRouter();
  const createPolicy = useCreatePolicy();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PolicyFormValues>({
    resolver: zodResolver(policySchema),
    defaultValues: {
      enabled: true,
    },
  });

  const onSubmit = async (data: PolicyFormValues) => {
    try {
      await createPolicy.mutateAsync(data);
      toast.success('Policy created successfully');
      router.push('/governance/policies');
    } catch {
      toast.error('Failed to create policy');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Policy"
        description="Create a new access policy"
        actions={
          <Link
            href="/governance/policies"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Policy Details</h3>

          <div>
            <label className="text-sm font-medium">Name *</label>
            <input
              {...register('name')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="e.g., SOD Policy for Finance"
            />
            {errors.name && (
              <p className="mt-1 text-xs text-destructive">{errors.name.message}</p>
            )}
          </div>

          <div>
            <label className="text-sm font-medium">Description</label>
            <textarea
              {...register('description')}
              rows={4}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="Describe the policy objectives and scope..."
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Policy Type *</label>
              <select
                {...register('policyType')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="">Select type...</option>
                <option value="ACCESS">Access</option>
                <option value="SEGREGATION_OF_DUTIES">Segregation of Duties</option>
                <option value="RECERTIFICATION">Recertification</option>
                <option value="RISK_BASED">Risk Based</option>
              </select>
              {errors.policyType && (
                <p className="mt-1 text-xs text-destructive">{errors.policyType.message}</p>
              )}
            </div>

            <div className="flex items-end">
              <label className="flex items-center gap-2 text-sm font-medium">
                <input
                  type="checkbox"
                  {...register('enabled')}
                  className="h-4 w-4 rounded border"
                />
                Enabled
              </label>
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {isSubmitting ? 'Creating...' : 'Create Policy'}
          </button>
          <Link
            href="/governance/policies"
            className="rounded-md border px-6 py-2 text-sm hover:bg-accent"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
