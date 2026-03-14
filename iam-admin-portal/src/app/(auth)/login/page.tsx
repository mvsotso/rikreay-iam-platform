"use client";

import { signIn } from "next-auth/react";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";

function LoginForm() {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get("callbackUrl") || "/dashboard";

  return (
    <div className="w-full max-w-md space-y-8 rounded-xl border bg-card p-8 shadow-lg">
      <div className="text-center">
        <h1 className="text-3xl font-bold tracking-tight">RikReay IAM</h1>
        <p className="mt-2 text-muted-foreground">
          Cambodia National IAM Platform
        </p>
      </div>
      <button
        onClick={() => signIn("keycloak", { callbackUrl })}
        className="w-full rounded-lg bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-sm hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-colors"
      >
        Sign in with Keycloak
      </button>
      <p className="text-center text-xs text-muted-foreground">
        Secured by Keycloak OIDC with PKCE
      </p>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
