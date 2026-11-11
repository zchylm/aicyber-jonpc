import coolingAir from "../assets/step07/original/cooling-tower-air.jpg";
import coolingDualAir from "../assets/step07/original/cooling-dual-tower-air.jpg";
import coolingLiquid from "../assets/step07/original/cooling-liquid-240.jpg";
import coolingLiquidRed from "../assets/step07/original/cooling-liquid-360.jpg";

export type CoolingOption = {
  id: string;
  label: string;
  detail: string;
  supportedCases: Array<"compact" | "mid" | "full">;
  recommendedFor: string;
  image: string;
};

export const coolingOptions: CoolingOption[] = [
  { id: "tower-air", label: "Tower air cooling", detail: "Quiet, efficient and easy to maintain", supportedCases: ["compact", "mid", "full"], recommendedFor: "Balanced everyday cooling", image: coolingAir },
  { id: "dual-tower-air", label: "Dual-tower air cooling", detail: "More thermal headroom for long sessions", supportedCases: ["mid", "full"], recommendedFor: "Higher sustained workloads", image: coolingDualAir },
  { id: "240-liquid", label: "240mm liquid cooling", detail: "Performance cooling with a clean internal layout", supportedCases: ["compact", "mid", "full"], recommendedFor: "Performance-focused builds", image: coolingLiquid },
  { id: "360-liquid", label: "360mm liquid cooling", detail: "Maximum cooling surface for demanding systems", supportedCases: ["mid", "full"], recommendedFor: "High-end sustained workloads", image: coolingLiquidRed },
];
