import type { DirectionId } from "./configurator";

export type StorageOption = {
  id: string;
  label: string;
  detail: string;
  price: number;
  recommendedFor: string;
};

export const storageOptions: StorageOption[] = [
  { id: "512gb", label: "512GB Gen4 NVMe", detail: "A focused drive for office apps, light gaming and everyday files", price: -160, recommendedFor: "Essential capacity" },
  { id: "1tb", label: "1TB Gen4 NVMe", detail: "Fast everyday storage for apps, games and active projects", price: -90, recommendedFor: "Focused starting point" },
  { id: "2tb", label: "2TB Gen4 NVMe", detail: "More space for modern games, media libraries and project files", price: 0, recommendedFor: "Recommended balance" },
  { id: "4tb", label: "4TB Gen4 NVMe", detail: "Large capacity for datasets, footage and specialist workflows", price: 160, recommendedFor: "More working room" },
];

export function recommendStorage(direction: DirectionId, answers: Record<string, string>) {
  const workload = answers.workload ?? answers.creativeWork ?? answers.use ?? "";
  const scale = answers.scale ?? answers.reliability ?? answers.priority ?? "";

  if (direction === "enterprise" && workload === "Office productivity") return "1tb";
  if (direction === "ai" && scale === "Heavy") return "4tb";
  if (direction === "workstation" && (workload === "Data processing" || workload === "Simulation")) return "4tb";
  return "2tb";
}

export function getStorageOption(id: string) {
  return storageOptions.find((option) => option.id === id) ?? storageOptions[0];
}

export function estimateStoragePrice(id: string) {
  return getStorageOption(id).price;
}
