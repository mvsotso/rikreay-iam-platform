"use client";

import { useRouter } from "next/navigation";
import { ShieldX } from "lucide-react";

export default function UnauthorizedPage() {
  const router = useRouter();

  return (
    <div className="w-full max-w-md space-y-6 rounded-xl border bg-card p-8 shadow-lg text-center">
      <ShieldX className="mx-auto h-16 w-16 text-destructive" />
      <h1 className="text-2xl font-bold">Access Denied</h1>
      <p className="text-muted-foreground">
        You do not have permission to access this resource.
      </p>
      <button
        onClick={() => router.push("/dashboard")}
        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
      >
        Go to Dashboard
      </button>
    </div>
  );
}
