export type AuthUser = {
  id: string;
  email: string;
  displayName: string;
  role: string;
};

export type AuthResponse = {
  accessToken: string;
  user: AuthUser;
};

export type RegisterRequest = {
  email: string;
  password: string;
  displayName: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");
export const authTokenKey = "jonpc.auth.token";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null;
    throw new Error(body?.detail ?? body?.message ?? `Authentication request failed with status ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export function register(requestBody: RegisterRequest): Promise<AuthResponse> {
  return requestJson<AuthResponse>("/api/auth/register", requestBody);
}

export function login(requestBody: LoginRequest): Promise<AuthResponse> {
  return requestJson<AuthResponse>("/api/auth/login", requestBody);
}

function requestJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: "POST", body: JSON.stringify(body) });
}

export function fetchCurrentUser(token: string): Promise<AuthUser> {
  return request<AuthUser>("/api/auth/me", {
    headers: { Authorization: `Bearer ${token}` },
  });
}
