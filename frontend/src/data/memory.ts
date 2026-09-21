import type { DirectionId } from "./configurator";

export type MemoryOption = {
  id: string;
  label: string;
  detail: string;
  price: number;
  recommendedFor: string;
};

export const memoryOptions: MemoryOption[] = [
  { id: "16gb", label: "16GB DDR5", detail: "2 x 8GB / Everyday productivity and entry gaming", price: 100, recommendedFor: "Essential starting point" },
  { id: "32gb", label: "32GB DDR5", detail: "2 x 16GB / Gaming, creation and multitasking", price: 200, recommendedFor: "Recommended balance" },
  { id: "64gb", label: "64GB DDR5", detail: "2 x 32GB / Heavy creation, AI and production work", price: 400, recommendedFor: "More headroom" },
  { id: "128gb", label: "128GB DDR5", detail: "High-capacity kit / Large datasets and specialist workloads", price: 800, recommendedFor: "Specialist capacity" },
];

export function recommendMemory(direction: DirectionId, answers: Record<string, string>) {
  const workload = answers.workload ?? answers.creativeWork ?? answers.use ?? "";
  const scale = answers.scale ?? answers.reliability ?? answers.priority ?? "";

  if (direction === "ai" && scale === "Heavy") return "128gb";
  if (direction === "workstation" && (workload === "Data processing" || workload === "Simulation")) return "64gb";
  if (direction === "creator" && (workload === "3D and motion" || answers.resolution === "8K")) return "64gb";
  if (direction === "enterprise" && workload === "Office productivity") return "16gb";
  return "32gb";
}

export function getMemoryOption(id: string) {
  return memoryOptions.find((option) => option.id === id) ?? memoryOptions[0];
}

export function estimateMemoryPrice(id: string) {
  return getMemoryOption(id).price;
}
