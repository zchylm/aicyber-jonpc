export type ConfiguratorQuoteRequest = {
  direction: string;
  answers: Record<string, string>;
  recommendedCpuId: string;
  recommendedGpuId: string;
  recommendedMemoryId: string;
  recommendedStorageId: string;
  recommendedMotherboardId: string;
  recommendedPsuId: string;
  recommendedCaseId: string;
  recommendedCoolingId: string;
  cpuId: string;
  gpuId: string;
  memoryId: string;
  storageId: string;
  motherboardId: string;
  psuId: string;
  caseId: string;
  coolingId: string;
};

export type ConfiguratorQuote = {
  recommendedBaseline: number;
  selectedAdjustments: number;
  estimatedTotal: number;
  compatible: boolean;
  budgetStatus: string;
  validation: string[];
};

export type ConfiguratorRecommendation = {
  cpuId: string;
  gpuId: string;
  memoryId: string;
  storageId: string;
  motherboardId: string;
  psuId: string;
  caseId: "compact" | "mid" | "full";
  coolingId: string;
};

export type ConfiguratorCompatibility = {
  motherboardIds: string[];
  psuIds: string[];
  caseIds: Array<"compact" | "mid" | "full">;
  coolingIds: string[];
  validation: string[];
};

export type ConfiguratorCatalogOption = {
  id: string;
  label: string;
  family: string;
  detail: string;
  price: number;
  platform?: "AM5" | "LGA1700" | string | null;
  chipset?: string | null;
  wifi?: boolean | null;
  formFactor?: "Micro-ATX" | "ATX" | string | null;
  efficiency?: string | null;
  wattage?: number | null;
  modular?: string | null;
  recommendedFor?: string | null;
  supportedCases: string[];
  formFactors: string[];
};

export type ConfiguratorCatalog = {
  systemBasePrice: number;
  options: Record<string, ConfiguratorCatalogOption[]>;
};

export type ConfiguratorBuildRequest = {
  name: string;
  email: string;
  phone: string;
  location: string;
  notes: string;
  contact: boolean;
  configuration: ConfiguratorQuoteRequest;
};

export type ConfiguratorBuildResponse = {
  requestReference: string;
  status: string;
  quote: ConfiguratorQuote;
  message: string;
};

export type SavedBuild = {
  id: string;
  name: string;
  direction: string;
  budgetRange: string | null;
  estimatedPrice: number;
  recommendedBaseline: number;
  selectedAdjustments: number;
  configuration: ConfiguratorQuoteRequest;
  status: string;
  createdAt: string;
  updatedAt: string;
};

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export async function fetchConfiguratorQuote(
  request: ConfiguratorQuoteRequest,
  signal?: AbortSignal,
): Promise<ConfiguratorQuote> {
  const response = await fetch(`${apiBaseUrl}/api/configurator/quote`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    signal,
  });

  if (!response.ok) {
    throw new Error(`Configurator quote failed with status ${response.status}`);
  }

  return response.json() as Promise<ConfiguratorQuote>;
}

export async function fetchConfiguratorRecommendation(
  direction: string,
  answers: Record<string, string>,
  signal?: AbortSignal,
): Promise<ConfiguratorRecommendation> {
  const response = await fetch(`${apiBaseUrl}/api/configurator/recommendation`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ direction, answers }),
    signal,
  });

  if (!response.ok) {
    throw new Error(`Configurator recommendation failed with status ${response.status}`);
  }

  return response.json() as Promise<ConfiguratorRecommendation>;
}

export async function fetchConfiguratorCompatibility(
  request: { cpuId: string; gpuId: string; motherboardId: string; psuId: string; caseId: "compact" | "mid" | "full"; coolingId: string },
  signal?: AbortSignal,
): Promise<ConfiguratorCompatibility> {
  const response = await fetch(`${apiBaseUrl}/api/configurator/compatibility`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    signal,
  });

  if (!response.ok) {
    throw new Error(`Configurator compatibility failed with status ${response.status}`);
  }

  return response.json() as Promise<ConfiguratorCompatibility>;
}

export async function fetchConfiguratorCatalog(signal?: AbortSignal): Promise<ConfiguratorCatalog> {
  const response = await fetch(`${apiBaseUrl}/api/configurator/catalog`, { signal });
  if (!response.ok) throw new Error(`Configurator catalog failed with status ${response.status}`);
  return response.json() as Promise<ConfiguratorCatalog>;
}

export async function submitConfiguratorBuild(request: ConfiguratorBuildRequest, signal?: AbortSignal): Promise<ConfiguratorBuildResponse> {
  const response = await fetch(`${apiBaseUrl}/api/configurator/requests`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    signal,
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string } | null;
    throw new Error(body?.detail ?? `Build request failed with status ${response.status}`);
  }
  return response.json() as Promise<ConfiguratorBuildResponse>;
}

export async function fetchSavedBuilds(token: string): Promise<SavedBuild[]> {
  const response = await fetch(`${apiBaseUrl}/api/builds`, { headers: { Authorization: `Bearer ${token}` } });
  if (!response.ok) throw new Error(`Saved builds failed with status ${response.status}`);
  return response.json() as Promise<SavedBuild[]>;
}

export async function saveBuild(token: string, request: {
  name: string;
  direction: string;
  budgetRange: string;
  estimatedPrice: number;
  recommendedBaseline: number;
  selectedAdjustments: number;
  configuration: ConfiguratorQuoteRequest;
}): Promise<SavedBuild> {
  const response = await fetch(`${apiBaseUrl}/api/builds`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(request),
  });
  const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null;
  if (!response.ok) throw new Error(body?.detail ?? body?.message ?? `Save build failed with status ${response.status}`);
  return body as SavedBuild;
}

export async function deleteSavedBuild(token: string, buildId: string): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/builds/${buildId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`Delete build failed with status ${response.status}`);
}
