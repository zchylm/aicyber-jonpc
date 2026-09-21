import type { PerformanceOption } from "./performance";

export type PsuOption = {
  id: string; label: string; efficiency: "Bronze" | "Gold" | "Platinum"; wattage: number;
  detail: string; price: number; modular: "Semi-modular" | "Fully modular";
};

export const psuOptions: PsuOption[] = [
  { id: "650-bronze", label: "650W 80+ Bronze", efficiency: "Bronze", wattage: 650, detail: "Reliable power for efficient graphics configurations", price: 110, modular: "Semi-modular" },
  { id: "750-gold", label: "750W 80+ Gold ATX 3.1", efficiency: "Gold", wattage: 750, detail: "Efficient current-generation power with upgrade room", price: 160, modular: "Fully modular" },
  { id: "850-gold", label: "850W 80+ Gold ATX 3.1", efficiency: "Gold", wattage: 850, detail: "High-performance power with additional headroom", price: 210, modular: "Fully modular" },
  { id: "1000-platinum", label: "1000W 80+ Platinum ATX 3.1", efficiency: "Platinum", wattage: 1000, detail: "Premium power for demanding flagship builds", price: 300, modular: "Fully modular" },
];

function minimumWattage(gpu: PerformanceOption) {
  if (gpu.id === "rtx-5090") return 1000;
  if (["rtx-5080", "rtx-5070-ti"].includes(gpu.id)) return 850;
  if (gpu.id === "rtx-5070") return 750;
  return 650;
}

export function getCompatiblePsus(gpu: PerformanceOption) {
  return psuOptions.filter((option) => option.wattage >= minimumWattage(gpu));
}

export function recommendPsu(gpu: PerformanceOption) {
  return getCompatiblePsus(gpu)[0].id;
}

export function getPsuOption(id: string, gpu: PerformanceOption) {
  return getCompatiblePsus(gpu).find((option) => option.id === id) ?? getCompatiblePsus(gpu)[0];
}
