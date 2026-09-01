import type { PerformanceOption } from "./performance";

export type PsuOption = {
  id: string;
  label: string;
  efficiency: "Standard" | "Bronze" | "Silver" | "Gold" | "Platinum" | "Titanium";
  wattage: number;
  detail: string;
  price: number;
  modular: "Non-modular" | "Semi-modular" | "Fully modular";
};

export const psuOptions: PsuOption[] = [
  { id: "550-standard", label: "550W 80+ Standard", efficiency: "Standard", wattage: 550, detail: "Entry-level power for efficient systems", price: -90, modular: "Non-modular" },
  { id: "550-bronze", label: "550W 80+ Bronze", efficiency: "Bronze", wattage: 550, detail: "Essential power for efficient systems", price: -60, modular: "Non-modular" },
  { id: "650-bronze", label: "650W 80+ Bronze", efficiency: "Bronze", wattage: 650, detail: "Budget-focused power for entry GPUs", price: 0, modular: "Semi-modular" },
  { id: "650-silver", label: "650W 80+ Silver", efficiency: "Silver", wattage: 650, detail: "Improved efficiency for everyday systems", price: 40, modular: "Semi-modular" },
  { id: "750-gold", label: "750W 80+ Gold", efficiency: "Gold", wattage: 750, detail: "Balanced efficiency and upgrade room", price: 100, modular: "Fully modular" },
  { id: "850-gold", label: "850W 80+ Gold", efficiency: "Gold", wattage: 850, detail: "High-performance power with headroom", price: 180, modular: "Fully modular" },
  { id: "1000-platinum", label: "1000W 80+ Platinum", efficiency: "Platinum", wattage: 1000, detail: "Quiet, efficient power for demanding builds", price: 350, modular: "Fully modular" },
  { id: "1200-titanium", label: "1200W 80+ Titanium", efficiency: "Titanium", wattage: 1200, detail: "Maximum efficiency and expansion capacity", price: 600, modular: "Fully modular" },
];

function minimumWattage(gpu: PerformanceOption) {
  if (gpu.id === "rtx-5080") return 850;
  if (gpu.id === "rtx-5070" || gpu.id === "rx-7800-xt") return 750;
  if (gpu.id === "rtx-4060" || gpu.id === "arc-b580") return 550;
  return 450;
}

export function getCompatiblePsus(gpu: PerformanceOption) {
  return psuOptions.filter((option) => option.wattage >= minimumWattage(gpu));
}

export function recommendPsu(gpu: PerformanceOption) {
  const minimum = minimumWattage(gpu);
  const target = minimum <= 550 ? 650 : minimum + 100;
  return getCompatiblePsus(gpu).find((option) => option.wattage >= target)?.id ?? getCompatiblePsus(gpu)[0].id;
}

export function getPsuOption(id: string, gpu: PerformanceOption) {
  return getCompatiblePsus(gpu).find((option) => option.id === id) ?? getCompatiblePsus(gpu)[0];
}
