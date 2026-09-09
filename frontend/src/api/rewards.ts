const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export type RewardPublicSummary = {
  available: boolean;
  programName: string | null;
  programStatus: string | null;
  currency: string;
  contributionRateBasisPoints: number | null;
  waitingCount: number;
  completedCount: number;
  totalAllocatedCents: number;
};

export type RewardEntry = {
  id: string;
  orderReference: string;
  queueSequence: number;
  currentPosition: number | null;
  targetAmountCents: number;
  allocatedAmountCents: number;
  remainingAmountCents: number;
  progressPercent: number;
  status: string;
  joinedAt: string;
  completedAt: string | null;
  latestAllocationAmountCents: number | null;
  latestAllocationAt: string | null;
};

export type RewardMemberSummary = {
  state: "PROGRAM_UNAVAILABLE" | "NO_ENTRY" | "ACTIVE";
  programStatus: string | null;
  currency: string;
  entries: RewardEntry[];
};

export type RewardDemoResponse = {
  action: string;
  message: string;
  summary: RewardPublicSummary;
  member: RewardMemberSummary;
};

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, options);
  const body = await response.json().catch(() => null) as (T & { detail?: string; message?: string }) | null;
  if (!response.ok) {
    throw new Error(body?.detail ?? body?.message ?? `Reward request failed with status ${response.status}`);
  }
  return body as T;
}

export function fetchRewardSummary(): Promise<RewardPublicSummary> {
  return request<RewardPublicSummary>("/api/rewards/summary");
}

export function fetchMyRewards(token: string): Promise<RewardMemberSummary> {
  return request<RewardMemberSummary>("/api/rewards/me", {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function qualifyLatestBuildRequest(token: string): Promise<RewardDemoResponse> {
  return demoAction("/api/rewards/demo/qualify-latest-request", token);
}

export function simulateIncomingOrder(token: string): Promise<RewardDemoResponse> {
  return demoAction("/api/rewards/demo/simulate-order", token);
}

export function resetRewardDemo(token: string): Promise<RewardDemoResponse> {
  return request<RewardDemoResponse>("/api/rewards/demo/reset", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ confirmation: "RESET_LOCAL_REWARD_DEMO" }),
  });
}

function demoAction(path: string, token: string): Promise<RewardDemoResponse> {
  return request<RewardDemoResponse>(path, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
}
