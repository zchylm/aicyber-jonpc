const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export type PreviewSystem = {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  priceCents: number;
  available: boolean;
  badge: string | null;
  recommendedFor: string | null;
  dispatchEstimate: string | null;
  specifications: Record<string, string>;
};

export async function fetchSystemPreviews(): Promise<PreviewSystem[]> {
  const response = await fetch(`${apiBaseUrl}/api/systems/preview`);
  if (!response.ok) throw new Error(`Unable to load systems (${response.status})`);
  return response.json() as Promise<PreviewSystem[]>;
}

export async function createLocalSystemRequest(token: string, sku: string, expectedPriceCents: number): Promise<{ requestReference: string; priceCents: number }> {
  const response = await fetch(`${apiBaseUrl}/api/systems/local-requests`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ sku, expectedPriceCents }),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null;
    throw new Error(body?.detail ?? body?.message ?? `Unable to request system (${response.status})`);
  }
  return response.json();
}
