export type PolicyId = "terms" | "shipping" | "returns" | "warranty" | "payment" | "custom" | "cashback" | "privacy";

export function openPolicy(policy: PolicyId) {
  window.dispatchEvent(new CustomEvent("jonpc:open-policy", { detail: { policy } }));
}
