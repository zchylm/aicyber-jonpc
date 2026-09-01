import type { DirectionId } from "./configurator";

export type MemoryOption = {
  id: string;
  label: string;
  detail: string;
  price: number;
  recommendedFor: string;
};

export const memoryOptions: MemoryOption[] = [
  { id: "16gb", label: "16GB DDR5", detail: "2 x 8GB / Everyday productivity and entry gaming", price: -120, recommendedFor: "Essential starting point" },
  { id: "32gb", label: "32GB DDR5", detail: "2 x 16GB / Smooth gaming and multitasking", price: 0, recommendedFor: "Recommended balance" },
  { id: "64gb", label: "64GB DDR5", detail: "2 x 32GB / Heavy creation, AI and production work", price: 160, recommendedFor: "More headroom" },
  { id: "128gb", label: "128GB DDR5", detail: "4 x 32GB / Large datasets and specialist workloads", price: 440, recommendedFor: "Maximum capacity" },
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
