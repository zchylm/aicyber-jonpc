import { authTokenKey } from "./auth";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export type AdminOverview = {
  totalOrders: number;
  pendingPaymentOrders: number;
  paidOrders: number;
  successfulPaymentCents: number;
  waitingRewards: number;
  completedRewards: number;
  totalAllocatedCents: number;
  failedRewardEvents: number;
  customReviewsNeeded: number;
  featuredSystemsSoldOut: number;
  lowStockComponents: number;
  inventorySkus: number;
  paymentIssues: number;
  invoiceEmailIssues: number;
  oldestCustomReviewHours: number;
  ordersToday: number;
  revenueTodayCents: number;
  rewardsAllocatedTodayCents: number;
  customRequestsToday: number;
  inventoryAlerts: Array<{ itemType: "SYSTEM" | "COMPONENT"; sku: string; name: string; availableQuantity: number; reorderPoint: number }>;
  recentActivity: Array<{ type: string; title: string; detail: string; amountCents: number | null; occurredAt: string }>;
};

export type AdminOrder = {
  id: string;
  orderReference: string;
  customerName: string;
  customerEmail: string;
  customerReference: string;
  amountCents: number;
  currency: string;
  status: string;
  paymentStatus: string | null;
  paymentReference: string | null;
  invoiceNumber: string | null;
  rewardStatus: string | null;
  createdAt: string;
};

export type AdminOrderDetail = {
  id: string;
  orderReference: string;
  customerName: string;
  customerEmail: string;
  customerReference: string;
  amountCents: number;
  currency: string;
  status: string;
  paidAt: string | null;
  rewardEligibleAt: string | null;
  createdAt: string;
  updatedAt: string;
  buildRequestReference: string | null;
  direction: string | null;
  configuration: Record<string, unknown>;
  delivery: null | {
    recipientName: string;
    phone: string;
    addressLine1: string;
    addressLine2: string | null;
    suburb: string;
    state: string;
    postcode: string;
    country: string;
  };
  payments: Array<{
    id: string;
    paymentReference: string;
    provider: string;
    providerPaymentId: string | null;
    amountCents: number;
    currency: string;
    status: string;
    failureReason: string | null;
    createdAt: string;
    succeededAt: string | null;
  }>;
  invoice: null | {
    id: string;
    invoiceNumber: string;
    status: string;
    subtotalExGstCents: number;
    gstCents: number;
    totalCents: number;
    issuedAt: string;
    deliveryStatus: string | null;
    sentAt: string | null;
  };
  reward: null | {
    id: string;
    founderNumber: number;
    tierName: string;
    rateBasisPoints: number;
    capCents: number;
    purchaseAmountCents: number;
    cashbackAmountCents: number;
    status: string;
    lockedAt: string;
    payableAt: string | null;
    paidAt: string | null;
  };
};

export type AdminOrderPage = { items: AdminOrder[]; total: number; page: number; size: number };

export type AdminRewards = {
  campaign: { maxPositions: number; confirmedPositions: number; availablePositions: number; maxLiabilityCents: number; committedCents: number; paidCents: number; availableBudgetCents: number };
  commitments: Array<{ id: string; founderNumber: number; tierName: string; orderReference: string; customerEmail: string; purchaseAmountCents: number; rateBasisPoints: number; capCents: number; cashbackAmountCents: number; status: string; lockedAt: string }>;
  failedEvents: Array<{ id: string; externalEventId: string; orderReference: string; attemptCount: number; lastError: string | null; createdAt: string }>;
};

export type CustomBuildReviewItem = {
  id: string;
  requestReference: string;
  name: string;
  email: string;
  phone: string | null;
  notes: string | null;
  direction: string;
  estimatedPrice: number;
  configuration: { answers?: Record<string, string>; [key: string]: unknown };
  createdAt: string;
  quoteId: string | null;
  quoteVersion: number;
  quoteStatus: string | null;
  totalCents: number;
  reviewNote: string | null;
  validUntil: string | null;
};

export function fetchCustomBuildReviews() {
  return request<CustomBuildReviewItem[]>("/api/admin/custom-build-review");
}

export function publishCustomBuildQuote(requestId: string, input: { totalCents: number; reviewNote: string; validUntil: string | null }) {
  return request<CustomBuildReviewItem>(`/api/admin/custom-build-review/${requestId}/quotes`, { method: "POST", body: JSON.stringify(input) });
}

export type AdminInventoryCategory = { id: string; code: string; name: string };
export type AdminInventoryLocation = { id: string; code: string; name: string; locationType: string; status: string; onHandQuantity: number; reservedQuantity: number };
export type AdminInventoryItem = {
  id: string;
  categoryCode: string;
  categoryName: string;
  sku: string;
  brand: string;
  model: string;
  displayName: string;
  description: string | null;
  specifications: Record<string, string | number | boolean>;
  unitCostCents: number | null;
  retailPriceCents: number;
  currency: string;
  status: string;
  onHandQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  updatedAt: string;
};
export type AdminInventoryMovement = {
  id: string;
  sku: string;
  itemName: string;
  locationCode: string;
  movementType: string;
  onHandDelta: number;
  reservedDelta: number;
  onHandAfter: number;
  reservedAfter: number;
  reason: string | null;
  performedBy: string | null;
  createdAt: string;
};
export type AdminInventory = {
  summary: { totalSkus: number; totalOnHand: number; totalReserved: number; lowStockSkus: number; inventoryCostCents: number };
  categories: AdminInventoryCategory[];
  locations: AdminInventoryLocation[];
  items: AdminInventoryItem[];
  recentMovements: AdminInventoryMovement[];
};
export type SystemProduct = {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  priceCents: number;
  plannedPreorderQuantity: number;
  onHandQuantity: number;
  reorderPoint: number;
  availableQuantity: number;
  listingOrder: number;
  productRange: string;
  salesMode: "STANDARD" | "PREORDER" | "STOCK_CHECK_REQUIRED";
  sourceRevision: string;
  badge: string | null;
  recommendedFor: string | null;
  dispatchEstimate: string | null;
  specifications: Record<string, string>;
  status: string;
  previewEnabled: boolean;
};
export type SystemProductInput = Omit<SystemProduct, "id" | "status" | "sourceRevision" | "onHandQuantity" | "reorderPoint" | "availableQuantity">;
export type InventoryItemInput = {
  categoryCode: string;
  sku: string;
  brand: string;
  model: string;
  displayName: string;
  description: string;
  specifications: Record<string, string>;
  unitCostCents: number | null;
  retailPriceCents: number;
  currency: string;
  status: string;
};

export function fetchAdminOverview() {
  return request<AdminOverview>("/api/admin/overview");
}

export function fetchAdminOrders(query = "", status = "") {
  const params = new URLSearchParams({ query, status });
  return request<AdminOrderPage>(`/api/admin/orders?${params}`);
}

export function fetchAdminOrder(orderId: string) {
  return request<AdminOrderDetail>(`/api/admin/orders/${orderId}`);
}

export async function downloadAdminInvoice(invoiceId: string, invoiceNumber: string) {
  const token = window.localStorage.getItem(authTokenKey);
  if (!token) throw new Error("Sign in required");
  const response = await fetch(`${apiBaseUrl}/api/admin/invoices/${invoiceId}/pdf`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`Unable to download invoice (${response.status})`);
  const url = URL.createObjectURL(await response.blob());
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${invoiceNumber}.pdf`;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function resendAdminInvoice(invoiceId: string) {
  return request<{ invoiceId: string; invoiceNumber: string; deliveryStatus: string }>(`/api/admin/invoices/${invoiceId}/resend`, { method: "POST" });
}

export function fetchAdminRewards() {
  return request<AdminRewards>("/api/admin/rewards");
}

export function fetchAdminInventory(query = "", category = "", status = "") {
  const params = new URLSearchParams({ query, category, status });
  return request<AdminInventory>(`/api/admin/inventory?${params}`);
}

export function fetchAdminSystems() {
  return request<SystemProduct[]>("/api/admin/systems");
}

export function createAdminSystem(input: SystemProductInput) {
  return request<SystemProduct>("/api/admin/systems", { method: "POST", body: JSON.stringify(input) });
}

export function updateAdminSystem(id: string, input: SystemProductInput) {
  return request<SystemProduct>(`/api/admin/systems/${id}`, { method: "PUT", body: JSON.stringify(input) });
}

export function receiveSystemStock(id: string, input: { quantity: number; reason: string; idempotencyKey: string }) {
  return request(`/api/admin/systems/${id}/receipts`, { method: "POST", body: JSON.stringify(input) });
}

export function adjustSystemStock(id: string, input: { quantity: number; reason: string; idempotencyKey: string }) {
  return request(`/api/admin/systems/${id}/adjustments`, { method: "POST", body: JSON.stringify(input) });
}

export function createInventoryItem(input: InventoryItemInput) {
  return request<AdminInventoryItem>("/api/admin/inventory/items", { method: "POST", body: JSON.stringify(input) });
}

export function updateInventoryItem(itemId: string, input: InventoryItemInput) {
  return request<AdminInventoryItem>(`/api/admin/inventory/items/${itemId}`, { method: "PUT", body: JSON.stringify(input) });
}

export function createInventoryLocation(input: { code: string; name: string; locationType: string }) {
  return request<AdminInventoryLocation>("/api/admin/inventory/locations", { method: "POST", body: JSON.stringify(input) });
}

export function receiveInventory(input: { inventoryItemId: string; locationId: string; quantity: number; reason: string; idempotencyKey: string }) {
  return request("/api/admin/inventory/receipts", { method: "POST", body: JSON.stringify(input) });
}

export function adjustInventory(input: { inventoryItemId: string; locationId: string; quantity: number; reason: string; idempotencyKey: string }) {
  return request("/api/admin/inventory/adjustments", { method: "POST", body: JSON.stringify(input) });
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = window.localStorage.getItem(authTokenKey);
  if (!token) throw new Error("Sign in required");
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${token}`, ...(init?.body ? { "Content-Type": "application/json" } : {}) },
  });
  const responseBody = await response.json().catch(() => null) as (T & { detail?: string; message?: string }) | null;
  if (!response.ok) {
    if (response.status === 403) throw new Error("Admin access required");
    throw new Error(responseBody?.detail ?? responseBody?.message ?? `Unable to load admin data (${response.status})`);
  }
  return responseBody as T;
}
