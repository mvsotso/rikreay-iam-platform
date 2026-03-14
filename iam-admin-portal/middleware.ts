import { auth } from "@/lib/auth";
import { NextResponse } from "next/server";

const ROUTE_ROLES: Record<string, string[]> = {
  "/identity": ["internal-user", "tenant-admin", "iam-admin"],
  "/users": ["iam-admin", "tenant-admin"],
  "/tenants": ["iam-admin"],
  "/monitoring": ["ops-admin", "iam-admin"],
  "/audit": ["auditor", "iam-admin", "report-viewer"],
  "/governance": ["governance-admin", "report-viewer"],
  "/developer": ["developer"],
  "/config": ["config-admin", "iam-admin"],
  "/notifications": ["iam-admin", "ops-admin"],
  "/xroad": ["service-manager", "iam-admin"],
  "/org": ["tenant-admin"],
  "/sector": ["sector-admin"],
};

export default auth((req) => {
  const { pathname } = req.nextUrl;

  // Public routes
  if (
    pathname.startsWith("/login") ||
    pathname.startsWith("/unauthorized") ||
    pathname.startsWith("/api/auth") ||
    pathname.startsWith("/_next") ||
    pathname === "/favicon.ico"
  ) {
    return NextResponse.next();
  }

  // Check authentication
  if (!req.auth) {
    const loginUrl = new URL("/login", req.url);
    loginUrl.searchParams.set("callbackUrl", pathname);
    return NextResponse.redirect(loginUrl);
  }

  // Check token refresh error
  if (req.auth.error === "RefreshAccessTokenError") {
    const loginUrl = new URL("/login", req.url);
    return NextResponse.redirect(loginUrl);
  }

  // Dashboard and profile are accessible to all authenticated users
  if (pathname === "/" || pathname.startsWith("/dashboard") || pathname.startsWith("/profile")) {
    return NextResponse.next();
  }

  // Role-based route protection
  const userRoles = req.auth.roles || [];
  for (const [route, allowedRoles] of Object.entries(ROUTE_ROLES)) {
    if (pathname.startsWith(route)) {
      const hasAccess = allowedRoles.some((role) => userRoles.includes(role));
      if (!hasAccess) {
        return NextResponse.redirect(new URL("/unauthorized", req.url));
      }
      break;
    }
  }

  return NextResponse.next();
});

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
