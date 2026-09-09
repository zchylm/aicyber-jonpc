import type { RewardEntry } from "./rewards";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export type MockPayment = {
  paymentId: string;
  paymentReference: string;
  orderId: string;
  orderReference: string;
  amountCents: number;
  currency: string;
  status: "CREATED" | "PROCESSING" | "SUCCEEDED" | "FAILED" | "CANCELLED";
  failureReason: string | null;
  rewardState: "NOT_ELIGIBLE" | "PROCESSING" | "JOINED";
  reward: RewardEntry | null;
};

export function createMockPayment(token: string, requestReference: string, idempotencyKey: string) {
  return request<MockPayment>("/api/payments/mock", token, {
    requestReference,
    idempotencyKey,
  });
}

export function completeMockPayment(token: string, paymentId: string, outcome: "SUCCEEDED" | "FAILED") {
  return request<MockPayment>(`/api/payments/mock/${paymentId}/complete`, token, { outcome });
}

async function request<T>(path: string, token: string, body: unknown): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const responseBody = await response.json().catch(() => null) as (T & { detail?: string; message?: string }) | null;
  if (!response.ok) {
    throw new Error(responseBody?.detail ?? responseBody?.message ?? `Payment request failed with status ${response.status}`);
  }
  return responseBody as T;
}
