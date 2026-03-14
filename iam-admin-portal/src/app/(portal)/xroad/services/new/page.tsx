'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateXRoadService } from '@/services/xroad';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const serviceSchema = z.object({
  serviceCode: z
    .string()
    .min(1, 'Service code is required')
    .max(100, 'Service code must be under 100 characters'),
  serviceName: z.string().min(1, 'Service name is required').max(200),
  memberClass: z.enum(['GOV', 'COM', 'NGO', 'MUN'], {
    required_error: 'Member class is required',
  }),
  memberCode: z.string().min(1, 'Member code is required'),
  subsystemCode: z.string().min(1, 'Subsystem code is required'),
  serviceVersion: z.string().optional(),
  url: z.string().url('Must be a valid URL'),
});

type ServiceFormValues = z.infer<typeof serviceSchema>;

export default function NewXRoadServicePage() {
  const router = useRouter();
  const createService = useCreateXRoadService();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceSchema),
  });

  const onSubmit = async (data: ServiceFormValues) => {
    try {
      await createService.mutateAsync({
        ...data,
        serviceVersion: data.serviceVersion || undefined,
      });
      toast.success('X-Road service registered successfully');
      router.push('/xroad');
    } catch {
      toast.error('Failed to register X-Road service');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Register X-Road Service"
        description="Register a new service on the X-Road Security Server"
        actions={
          <Link
            href="/xroad"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Service Identity</h3>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Service Code *</label>
              <input
                {...register('serviceCode')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="e.g., getPersonData"
              />
              {errors.serviceCode && (
                <p className="mt-1 text-xs text-destructive">{errors.serviceCode.message}</p>
              )}
            </div>

            <div>
              <label className="text-sm font-medium">Service Version</label>
              <input
                {...register('serviceVersion')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="e.g., v1"
              />
            </div>
          </div>

          <div>
            <label className="text-sm font-medium">Service Name *</label>
            <input
              {...register('serviceName')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="e.g., Get Person Data Service"
            />
            {errors.serviceName && (
              <p className="mt-1 text-xs text-destructive">{errors.serviceName.message}</p>
            )}
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">X-Road Member</h3>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Member Class *</label>
              <select
                {...register('memberClass')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="">Select class...</option>
                <option value="GOV">GOV - Government</option>
                <option value="COM">COM - Commercial</option>
                <option value="NGO">NGO - Non-Governmental</option>
                <option value="MUN">MUN - Municipal</option>
              </select>
              {errors.memberClass && (
                <p className="mt-1 text-xs text-destructive">{errors.memberClass.message}</p>
              )}
            </div>

            <div>
              <label className="text-sm font-medium">Member Code *</label>
              <input
                {...register('memberCode')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="e.g., GDT"
              />
              {errors.memberCode && (
                <p className="mt-1 text-xs text-destructive">{errors.memberCode.message}</p>
              )}
            </div>
          </div>

          <div>
            <label className="text-sm font-medium">Subsystem Code *</label>
            <input
              {...register('subsystemCode')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="e.g., IDENTITY"
            />
            {errors.subsystemCode && (
              <p className="mt-1 text-xs text-destructive">{errors.subsystemCode.message}</p>
            )}
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Service Endpoint</h3>

          <div>
            <label className="text-sm font-medium">Service URL *</label>
            <input
              {...register('url')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="https://service.example.com/api/v1"
            />
            {errors.url && (
              <p className="mt-1 text-xs text-destructive">{errors.url.message}</p>
            )}
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {isSubmitting ? 'Registering...' : 'Register Service'}
          </button>
          <Link
            href="/xroad"
            className="rounded-md border px-6 py-2 text-sm hover:bg-accent"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
