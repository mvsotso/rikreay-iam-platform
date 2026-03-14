'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateTemplate } from '@/services/notifications';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const templateSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  channel: z.enum(['EMAIL', 'SMS', 'TELEGRAM']),
  subject: z.string().min(1, 'Subject is required'),
  body: z.string().min(1, 'Body is required'),
  variables: z.string().optional(),
});

type TemplateFormValues = z.infer<typeof templateSchema>;

export default function NewTemplatePage() {
  const router = useRouter();
  const createTemplate = useCreateTemplate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<TemplateFormValues>({
    resolver: zodResolver(templateSchema),
    defaultValues: { channel: 'EMAIL', variables: '' },
  });

  const onSubmit = async (data: TemplateFormValues) => {
    try {
      const variables = data.variables
        ? data.variables
            .split(',')
            .map((v) => v.trim())
            .filter(Boolean)
        : [];
      await createTemplate.mutateAsync({
        name: data.name,
        channel: data.channel,
        subject: data.subject,
        body: data.body,
        variables,
        active: true,
      });
      toast.success('Notification template created');
      router.push('/notifications/templates');
    } catch {
      toast.error('Failed to create template');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Template"
        description="Create a new notification template"
        actions={
          <Link
            href="/notifications/templates"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Template Details</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Name *</label>
              <input
                {...register('name')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="e.g. Welcome Email"
              />
              {errors.name && (
                <p className="mt-1 text-xs text-destructive">{errors.name.message}</p>
              )}
            </div>
            <div>
              <label className="text-sm font-medium">Channel *</label>
              <select
                {...register('channel')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS</option>
                <option value="TELEGRAM">Telegram</option>
              </select>
            </div>
            <div className="col-span-2">
              <label className="text-sm font-medium">Subject *</label>
              <input
                {...register('subject')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="e.g. Welcome to {{orgName}}"
              />
              {errors.subject && (
                <p className="mt-1 text-xs text-destructive">{errors.subject.message}</p>
              )}
            </div>
            <div className="col-span-2">
              <label className="text-sm font-medium">Body *</label>
              <textarea
                {...register('body')}
                rows={8}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm font-mono"
                placeholder="Hello {{userName}}, welcome to {{orgName}}..."
              />
              {errors.body && (
                <p className="mt-1 text-xs text-destructive">{errors.body.message}</p>
              )}
            </div>
            <div className="col-span-2">
              <label className="text-sm font-medium">Variables</label>
              <input
                {...register('variables')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="userName, orgName, resetLink (comma-separated)"
              />
              <p className="mt-1 text-xs text-muted-foreground">
                Comma-separated list of template variable names
              </p>
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {isSubmitting ? 'Creating...' : 'Create Template'}
          </button>
          <Link
            href="/notifications/templates"
            className="rounded-md border px-6 py-2 text-sm hover:bg-accent"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
