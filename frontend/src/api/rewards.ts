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
  status: "PROCESSING" | "PAID" | "VOID";
  lockedAt: string;
  paidAt: string | null;
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

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, options);
  const body = await response.json().catch(() => null) as (T & { detail?: string; message?: string }) | null;
  if (!response.ok) throw new Error(body?.detail ?? body?.message ?? `Reward request failed with status ${response.status}`);
  return body as T;
}

export const fetchRewardSummary = () => request<RewardPublicSummary>("/api/rewards/summary");
export const fetchMyRewards = (token: string) => request<RewardMemberSummary>("/api/rewards/me", { headers: { Authorization: `Bearer ${token}` } });
