export type PsuOption = {
  id: string; label: string; efficiency: "Bronze" | "Gold" | "Platinum"; wattage: number;
  detail: string; modular: "Semi-modular" | "Fully modular";
};

export const psuOptions: PsuOption[] = [
  { id: "650-bronze", label: "650W 80+ Bronze", efficiency: "Bronze", wattage: 650, detail: "Reliable power for efficient graphics configurations", modular: "Semi-modular" },
  { id: "750-gold", label: "750W 80+ Gold ATX 3.1", efficiency: "Gold", wattage: 750, detail: "Efficient current-generation power with upgrade room", modular: "Fully modular" },
  { id: "850-gold", label: "850W 80+ Gold ATX 3.1", efficiency: "Gold", wattage: 850, detail: "High-performance power with additional headroom", modular: "Fully modular" },
  { id: "1000-platinum", label: "1000W 80+ Platinum ATX 3.1", efficiency: "Platinum", wattage: 1000, detail: "Premium power for demanding flagship builds", modular: "Fully modular" },
];
