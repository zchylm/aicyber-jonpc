import type { PerformanceOption } from "./performance";
import coolingAir from "../assets/step07/cooling-air.jpeg";
import coolingLiquid from "../assets/step07/cooling-liquid.jpeg";
import coolingLiquidRed from "../assets/step07/cooling-liquid-red.jpeg";

export type CoolingOption = {
  id: string;
  label: string;
  detail: string;
  price: number;
  supportedCases: Array<"compact" | "mid" | "full">;
  recommendedFor: string;
  image: string;
};

export const coolingOptions: CoolingOption[] = [
  { id: "tower-air", label: "Tower air cooling", detail: "Quiet, efficient and easy to maintain", price: 70, supportedCases: ["compact", "mid", "full"], recommendedFor: "Balanced everyday cooling", image: coolingAir },
  { id: "dual-tower-air", label: "Dual-tower air cooling", detail: "More thermal headroom for long sessions", price: 110, supportedCases: ["mid", "full"], recommendedFor: "Higher sustained workloads", image: coolingAir },
  { id: "240-liquid", label: "240mm liquid cooling", detail: "Performance cooling with a clean internal layout", price: 150, supportedCases: ["compact", "mid", "full"], recommendedFor: "Performance-focused builds", image: coolingLiquid },
  { id: "360-liquid", label: "360mm liquid cooling", detail: "Maximum cooling surface for demanding systems", price: 200, supportedCases: ["mid", "full"], recommendedFor: "High-end sustained workloads", image: coolingLiquidRed },
];

export function getCompatibleCooling(cpu: PerformanceOption, caseType: "compact" | "mid" | "full") {
  const highHeatCpu = ["core-ultra-7-265k", "ryzen-7-9850x3d", "ryzen-9-9900x", "ryzen-9-9950x3d"].includes(cpu.id);
  return coolingOptions.filter((option) => option.supportedCases.includes(caseType) && !(highHeatCpu && option.id === "tower-air"));
}

export function recommendCooling(cpu: PerformanceOption, caseType: "compact" | "mid" | "full") {
  const options = getCompatibleCooling(cpu, caseType);
  if (cpu.id === "ryzen-9-9950x3d") return options.find((option) => option.id === "360-liquid")?.id ?? options[0].id;
  if (["core-ultra-7-265k", "ryzen-7-9850x3d", "ryzen-9-9900x"].includes(cpu.id)) return options.find((option) => option.id === "240-liquid")?.id ?? options[0].id;
  if (caseType === "compact") return options.find((option) => option.id === "tower-air")?.id ?? options[0].id;
  return options.find((option) => option.id === "dual-tower-air")?.id ?? options[0].id;
}

export function getCoolingOption(id: string, cpu: PerformanceOption, caseType: "compact" | "mid" | "full") {
  return getCompatibleCooling(cpu, caseType).find((option) => option.id === id) ?? getCompatibleCooling(cpu, caseType)[0];
}
