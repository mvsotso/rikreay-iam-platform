'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateWebhook } from '@/services/developer';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const AVAILABLE_EVENTS = [
  'user.created',
  'user.updated',
  'user.deleted',
  'tenant.created',
  'tenant.updated',
  'app.registered',
  'app.suspended',
  'identity.verified',
  'representation.created',
  'representation.revoked',
];

const webhookSchema = z.object({
  appId: z.string().min(1, 'App ID is required'),
  url: z.string().url('Must be a valid URL'),
  events: z.string().min(1, 'At least one event is required'),
  secret: z.string().optional(),
});

type WebhookFormValues = z.infer<typeof webhookSchema>;

export default function NewWebhookPage() {
  const router = useRouter();
  const createWebhook = useCreateWebhook();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<WebhookFormValues>({
    resolver: zodResolver(webhookSchema),
  });

  const onSubmit = async (data: WebhookFormValues) => {
    try {
      const events = data.events
        .split(',')
        .map((e) => e.trim())
        .filter(Boolean);
      await createWebhook.mutateAsync({
        appId: data.appId,
        url: data.url,
        events,
        secret: data.secret || undefined,
      });
      toast.success('Webhook created successfully');
      router.push('/developer/webhooks');
    } catch {
      toast.error('Failed to create webhook');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Webhook"
        description="Create a new webhook subscription"
        actions={
          <Link
            href="/developer/webhooks"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Webhook Details</h3>

          <div>
            <label className="text-sm font-medium">App ID *</label>
            <input
              {...register('appId')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="Application ID this webhook belongs to"
            />
            {errors.appId && (
              <p className="mt-1 text-xs text-destructive">{errors.appId.message}</p>
            )}
          </div>

          <div>
            <label className="text-sm font-medium">URL *</label>
            <input
              {...register('url')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="https://your-app.com/webhook"
            />
            {errors.url && (
              <p className="mt-1 text-xs text-destructive">{errors.url.message}</p>
            )}
          </div>

          <div>
            <label className="text-sm font-medium">Events *</label>
            <input
              {...register('events')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="user.created, user.updated, tenant.created"
            />
            <p className="mt-1 text-xs text-muted-foreground">
              Comma-separated. Available: {AVAILABLE_EVENTS.join(', ')}
            </p>
            {errors.events && (
              <p className="mt-1 text-xs text-destructive">{errors.events.message}</p>
            )}
          </div>

          <div>
            <label className="text-sm font-medium">Secret</label>
            <input
              {...register('secret')}
              type="password"
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="Optional signing secret for payload verification"
            />
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {isSubmitting ? 'Creating...' : 'Create Webhook'}
          </button>
          <Link
            href="/developer/webhooks"
            className="rounded-md border px-6 py-2 text-sm hover:bg-accent"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
