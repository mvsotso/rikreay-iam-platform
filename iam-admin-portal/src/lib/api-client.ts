import { getSession } from "next-auth/react";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
  errors?: FieldError[];
  timestamp: string;
  requestId: string;
  path?: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public errorCode: string | undefined,
    public errors: FieldError[] | undefined,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function getAuthHeaders(): Promise<HeadersInit> {
  const session = await getSession();
  const headers: HeadersInit = {
    "Content-Type": "application/json",
  };
  if (session?.accessToken) {
    headers["Authorization"] = `Bearer ${session.accessToken}`;
  }
  return headers;
}

export async function apiClient<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = await getAuthHeaders();

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { ...headers, ...options.headers },
  });

  if (response.status === 401) {
    window.location.href = "/login";
    throw new ApiError(401, "UNAUTHORIZED", undefined, "Session expired");
  }

  if (response.status === 403) {
    throw new ApiError(403, "FORBIDDEN", undefined, "Access denied");
  }

  const json: ApiResponse<T> = await response.json();

  if (!json.success) {
    throw new ApiError(
      response.status,
      json.errorCode,
      json.errors,
      json.message
    );
  }

  return json.data;
}

// Convenience methods
export const api = {
  get: <T>(path: string) => apiClient<T>(path),
  post: <T>(path: string, body: unknown) =>
    apiClient<T>(path, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    apiClient<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  delete: <T>(path: string) =>
    apiClient<T>(path, { method: "DELETE" }),
  patch: <T>(path: string, body: unknown) =>
    apiClient<T>(path, { method: "PATCH", body: JSON.stringify(body) }),
};

// Pagination helper: converts TanStack Table state to Spring Data Pageable params
export function buildPageParams(params: Record<string, string | number | boolean | undefined>): string {
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") {
      searchParams.set(key, String(value));
    }
  }
  return searchParams.toString();
}
