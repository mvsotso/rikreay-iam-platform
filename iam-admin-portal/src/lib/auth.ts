import NextAuth from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";
import type { NextAuthConfig } from "next-auth";
import type { JWT } from "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    roles: string[];
    error?: string;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    roles?: string[];
    error?: string;
  }
}

async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const response = await fetch(
      `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`,
      {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          client_id: process.env.KEYCLOAK_CLIENT_ID!,
          grant_type: "refresh_token",
          refresh_token: token.refreshToken!,
        }),
      }
    );

    const refreshed = await response.json();

    if (!response.ok) {
      throw new Error(refreshed.error || "Failed to refresh token");
    }

    return {
      ...token,
      accessToken: refreshed.access_token,
      refreshToken: refreshed.refresh_token ?? token.refreshToken,
      expiresAt: Math.floor(Date.now() / 1000) + refreshed.expires_in,
      roles: extractRoles(refreshed.access_token),
      error: undefined,
    };
  } catch {
    return { ...token, error: "RefreshAccessTokenError" };
  }
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const json = Buffer.from(base64, "base64").toString("utf-8");
    return JSON.parse(json);
  } catch {
    return {};
  }
}

function extractRoles(accessToken: string | undefined): string[] {
  if (!accessToken) return [];
  const payload = decodeJwtPayload(accessToken);
  const roles: string[] = [];
  const realmAccess = payload.realm_access as { roles?: string[] } | undefined;
  if (realmAccess?.roles) {
    roles.push(...realmAccess.roles);
  }
  return roles;
}

export const authConfig: NextAuthConfig = {
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      issuer: process.env.KEYCLOAK_ISSUER!,
      authorization: {
        params: {
          scope: "openid profile email",
        },
      },
    }),
  ],
  callbacks: {
    async jwt({ token, account }) {
      // Initial sign-in
      if (account) {
        return {
          ...token,
          accessToken: account.access_token,
          refreshToken: account.refresh_token,
          expiresAt: account.expires_at,
          roles: extractRoles(account.access_token),
        };
      }

      // Token not expired yet
      if (token.expiresAt && Date.now() / 1000 < token.expiresAt - 60) {
        return token;
      }

      // Token expired, try to refresh
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      session.roles = token.roles ?? [];
      session.error = token.error;
      return session;
    },
  },
  pages: {
    signIn: "/login",
    error: "/login",
  },
  session: {
    strategy: "jwt",
    maxAge: 8 * 60 * 60, // 8 hours
  },
};

export const { handlers, auth, signIn, signOut } = NextAuth(authConfig);
