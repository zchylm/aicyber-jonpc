import type { DirectionId } from "./configurator";

export type PerformanceOption = {
  id: string;
  label: string;
  family: string;
  detail: string;
  price: number;
  platform?: "AM5" | "LGA1851";
};

export type PerformanceSelection = {
  cpuId: string;
  gpuId: string;
};

export type BudgetRange = {
  min: number;
  max: number;
  label: string;
};

export const gpuOptions: PerformanceOption[] = [
  { id: "integrated", label: "Integrated graphics", family: "AMD / Intel", detail: "Office, display and everyday productivity", price: 0 },
  { id: "rtx-5060", label: "NVIDIA GeForce RTX 5060 8GB", family: "NVIDIA GeForce RTX 50", detail: "Current-generation 1080p gaming and creator acceleration", price: 649 },
  { id: "rtx-5060-ti-16gb", label: "NVIDIA GeForce RTX 5060 Ti 16GB", family: "NVIDIA GeForce RTX 50", detail: "More VRAM for 1440p, creation and local AI", price: 949 },
  { id: "rtx-5070", label: "NVIDIA GeForce RTX 5070 12GB", family: "NVIDIA GeForce RTX 50", detail: "Balanced 1440p, AI and creator performance", price: 1249 },
  { id: "rtx-5070-ti", label: "NVIDIA GeForce RTX 5070 Ti 16GB", family: "NVIDIA GeForce RTX 50", detail: "High-refresh 1440p and stronger compute headroom", price: 1749 },
  { id: "rtx-5080", label: "NVIDIA GeForce RTX 5080 16GB", family: "NVIDIA GeForce RTX 50", detail: "High-end 4K and local compute performance", price: 2199 },
  { id: "rtx-5090", label: "NVIDIA GeForce RTX 5090 32GB", family: "NVIDIA GeForce RTX 50", detail: "Specialist flagship graphics and AI capacity", price: 7999 },
];

export const cpuOptions: PerformanceOption[] = [
  { id: "ryzen-5-7500f", label: "AMD Ryzen 5 7500F", family: "AMD Ryzen", detail: "Efficient 6-core AM5 value choice", price: 239, platform: "AM5" },
  { id: "ryzen-5-9600x", label: "AMD Ryzen 5 9600X", family: "AMD Ryzen", detail: "Current-generation gaming and everyday performance", price: 348, platform: "AM5" },
  { id: "core-ultra-5-245k", label: "Intel Core Ultra 5 245K", family: "Intel Core Ultra", detail: "Balanced current-generation productivity platform", price: 329, platform: "LGA1851" },
  { id: "ryzen-7-9700x", label: "AMD Ryzen 7 9700X", family: "AMD Ryzen", detail: "Efficient 8-core performance for mixed workloads", price: 455, platform: "AM5" },
  { id: "core-ultra-7-265k", label: "Intel Core Ultra 7 265K", family: "Intel Core Ultra", detail: "Strong multi-core performance for creation and development", price: 565, platform: "LGA1851" },
  { id: "ryzen-7-9850x3d", label: "AMD Ryzen 7 9850X3D", family: "AMD Ryzen", detail: "Gaming-focused performance with high frame consistency", price: 744, platform: "AM5" },
  { id: "ryzen-9-9900x", label: "AMD Ryzen 9 9900X", family: "AMD Ryzen", detail: "12-core capacity for creation, AI and workstation work", price: 629, platform: "AM5" },
  { id: "ryzen-9-9950x3d", label: "AMD Ryzen 9 9950X3D", family: "AMD Ryzen", detail: "Flagship gaming and heavy multi-core performance", price: 1029, platform: "AM5" },
];

const findOption = (options: PerformanceOption[], id: string) => options.find((option) => option.id === id) ?? options[0];

export function getBudgetRange(direction: DirectionId, answers: Record<string, string>): BudgetRange {
  const budget = answers.budget ?? "";
  const match = budget.match(/\$(\d{1,3}(?:,\d{3})?)[–-]\$(\d{1,3}(?:,\d{3})?)/);
  if (match) {
    return { min: Number(match[1].replace(",", "")), max: Number(match[2].replace(",", "")), label: budget };
  }

  const plusMatch = budget.match(/\$(\d{1,3}(?:,\d{3})?)\+/);
  if (plusMatch) {
    const min = Number(plusMatch[1].replace(",", ""));
    return { min, max: min + 1800, label: `${budget} direction` };
  }

  const defaults: Record<DirectionId, BudgetRange> = {
    gaming: { min: 2800, max: 3800, label: "$2,800–$3,800" },
    ai: { min: 4000, max: 5500, label: "$4,000–$5,500" },
    creator: { min: 3200, max: 4500, label: "$3,200–$4,500" },
    workstation: { min: 4000, max: 6000, label: "$4,000–$6,000" },
    enterprise: { min: 2300, max: 3500, label: "$2,300–$3,500" },
  };
  return defaults[direction];
}

export function estimateCorePrice(selection: PerformanceSelection) {
  return 650 + findOption(cpuOptions, selection.cpuId).price + findOption(gpuOptions, selection.gpuId).price;
}
