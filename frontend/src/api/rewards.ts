const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export type RewardPublicSummary = {
  available: boolean;
  programName: string | null;
  programStatus: string | null;
  currency: string;
  maxPositions: number;
  confirmedCount: number;
  remainingPositions: number;
  currentTierName: string | null;
  currentRateBasisPoints: number | null;
  currentCapCents: number | null;
  currentTierRemaining: number;
};

export type RewardEntry = {
  id: string;
  orderReference: string;
  founderNumber: number;
  tierName: string;
  rateBasisPoints: number;
  capCents: number;
  purchaseAmountCents: number;
  cashbackAmountCents: number;
  effectivePriceCents: number;
  status: "LOCKED" | "PAYABLE" | "PROCESSING" | "PAID" | "FAILED" | "VOID";
  lockedAt: string;
  payableAt: string | null;
  payoutDueAt: string | null;
  processingAt: string | null;
  paidAt: string | null;
  payoutMethod: string;
  payoutReference: string | null;
  payoutFailureReason: string | null;
};

export type RewardCheckoutPreview = {
  available: boolean;
  reason: string | null;
  tierName: string | null;
  rateBasisPoints: number | null;
  capCents: number | null;
  cashbackAmountCents: number | null;
  effectivePriceCents: number | null;
  remainingPositions: number;
};

export type RewardMemberSummary = {
  state: "PROGRAM_UNAVAILABLE" | "NO_ENTRY" | "ACTIVE";
  programStatus: string | null;
  currency: string;
  entries: RewardEntry[];
};

export type RewardDemoResponse = { action: string; message: string; summary: RewardPublicSummary; member: RewardMemberSummary };

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, options);
  const body = await response.json().catch(() => null) as (T & { detail?: string; message?: string }) | null;
  if (!response.ok) throw new Error(body?.detail ?? body?.message ?? `Reward request failed with status ${response.status}`);
  return body as T;
}

export const fetchRewardSummary = () => request<RewardPublicSummary>("/api/rewards/summary");
export const fetchMyRewards = (token: string) => request<RewardMemberSummary>("/api/rewards/me", { headers: { Authorization: `Bearer ${token}` } });
export const qualifyLatestBuildRequest = (token: string) => demoAction("/api/rewards/demo/qualify-latest-request", token);
export const createFounderDemoOrder = (token: string) => demoAction("/api/rewards/demo/create-founder-order", token);
export const makeCashbackPayable = (token: string) => demoAction("/api/rewards/demo/make-cashback-payable", token);
export const markCashbackPaid = (token: string) => demoAction("/api/rewards/demo/mark-cashback-paid", token);

export function resetRewardDemo(token: string) {
  return request<RewardDemoResponse>("/api/rewards/demo/reset", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ confirmation: "RESET_LOCAL_REWARD_DEMO" }),
  });
}

function demoAction(path: string, token: string) {
  return request<RewardDemoResponse>(path, { method: "POST", headers: { Authorization: `Bearer ${token}` } });
}
