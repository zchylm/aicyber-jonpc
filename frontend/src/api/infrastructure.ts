import { authTokenKey } from "./auth";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export type InfrastructureRequestResponse = {
  requestReference: string;
  status: string;
  message: string;
};

export async function submitInfrastructureRequest(input: {
  organisation: string;
  phone: string;
  location: string;
  notes: string;
  contact: boolean;
  configuration: Record<string, string>;
}): Promise<InfrastructureRequestResponse> {
  const token = window.localStorage.getItem(authTokenKey);
  if (!token) throw new Error("Log in before submitting this project.");
  const response = await fetch(`${apiBaseUrl}/api/infrastructure/requests`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(input),
  });
  const body = await response.json().catch(() => null) as (InfrastructureRequestResponse & { detail?: string }) | null;
  if (!response.ok) throw new Error(body?.detail ?? `Infrastructure request failed with status ${response.status}`);
  return body as InfrastructureRequestResponse;
}
