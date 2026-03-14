'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateIncident } from '@/services/monitoring';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const incidentSchema = z.object({
  title: z.string().min(1, 'Title is required').max(200, 'Title must be under 200 characters'),
  description: z.string().min(1, 'Description is required'),
  severity: z.enum(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'], {
    required_error: 'Severity is required',
  }),
  serviceName: z.string().min(1, 'Service name is required'),
  assignedTo: z.string().optional(),
});

type IncidentFormValues = z.infer<typeof incidentSchema>;

export default function NewIncidentPage() {
  const router = useRouter();
  const createIncident = useCreateIncident();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<IncidentFormValues>({
    resolver: zodResolver(incidentSchema),
  });

  const onSubmit = async (data: IncidentFormValues) => {
    try {
      const cleaned = { ...data, assignedTo: data.assignedTo || undefined };
      await createIncident.mutateAsync(cleaned);
      toast.success('Incident created successfully');
      router.push('/monitoring/incidents');
    } catch {
      toast.error('Failed to create incident');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Incident"
        description="Report a new platform incident"
        actions={
          <Link
            href="/monitoring/incidents"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Incident Details</h3>

          <div>
            <label className="text-sm font-medium">Title *</label>
            <input
              {...register('title')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="Brief description of the incident"
            />
            {errors.title && (
              <p className="mt-1 text-xs text-destructive">{errors.title.message}</p>
            )}
          </div>

          <div>
            <label className="text-sm font-medium">Description *</label>
            <textarea
              {...register('description')}
              rows={5}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="Detailed description of the incident, impact, and any relevant context..."
            />
            {errors.description && (
              <p className="mt-1 text-xs text-destructive">{errors.description.message}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Severity *</label>
              <select
                {...register('severity')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="">Select severity...</option>
                <option value="CRITICAL">Critical</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
              {errors.severity && (
                <p className="mt-1 text-xs text-destructive">{errors.severity.message}</p>
              )}
            </div>

            <div>
              <label className="text-sm font-medium">Service Name *</label>
              <input
                {...register('serviceName')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="e.g., iam-core-service"
              />
              {errors.serviceName && (
                <p className="mt-1 text-xs text-destructive">{errors.serviceName.message}</p>
              )}
            </div>
          </div>

          <div>
            <label className="text-sm font-medium">Assigned To</label>
            <input
              {...register('assignedTo')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="Username or team (optional)"
            />
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {isSubmitting ? 'Creating...' : 'Create Incident'}
          </button>
          <Link
            href="/monitoring/incidents"
            className="rounded-md border px-6 py-2 text-sm hover:bg-accent"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
