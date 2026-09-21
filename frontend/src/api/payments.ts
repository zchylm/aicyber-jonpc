import type { RewardCheckoutPreview, RewardEntry } from "./rewards";

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
  invoiceId: string | null;
  invoiceNumber: string | null;
  rewardState: "NOT_ELIGIBLE" | "PROCESSING" | "JOINED";
  rewardPreview: RewardCheckoutPreview | null;
  reward: RewardEntry | null;
};

export type DeliveryDetails = {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  suburb: string;
  state: string;
  postcode: string;
};

export async function fetchDeliveryDetails(token: string, reference: string): Promise<DeliveryDetails | null> {
  const response = await fetch(`${apiBaseUrl}/api/orders/delivery/${encodeURIComponent(reference)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`Unable to load delivery address (${response.status})`);
  if (response.status === 204) return null;
  const body = await response.text();
  return body ? JSON.parse(body) as DeliveryDetails : null;
}

export async function saveDeliveryDetails(token: string, reference: string, details: DeliveryDetails): Promise<DeliveryDetails> {
  const response = await fetch(`${apiBaseUrl}/api/orders/delivery/${encodeURIComponent(reference)}`, {
    method: "PUT",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify(details),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.detail ?? body?.message ?? `Unable to save delivery address (${response.status})`);
  }
  return response.json();
}

export function createMockPayment(token: string, requestReference: string, idempotencyKey: string) {
  return request<MockPayment>("/api/payments/mock", token, {
    requestReference,
    idempotencyKey,
  });
}

export function completeMockPayment(token: string, paymentId: string, outcome: "SUCCEEDED" | "FAILED") {
  return request<MockPayment>(`/api/payments/mock/${paymentId}/complete`, token, { outcome });
}

export async function downloadInvoicePdf(token: string, invoiceId: string, invoiceNumber: string) {
  const response = await fetch(`${apiBaseUrl}/api/invoices/${invoiceId}/pdf`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`Invoice download failed with status ${response.status}`);
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = `${invoiceNumber}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
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
