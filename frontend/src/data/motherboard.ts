import type { PerformanceOption } from "./performance";

export type MotherboardOption = {
  id: string; label: string; platform: "AM5" | "LGA1851"; chipset: string; detail: string;
  price: number; wifi: boolean; formFactor: "Micro-ATX" | "ATX";
};

export const motherboardOptions: MotherboardOption[] = [
  { id: "b850m-wifi", label: "B850M Gaming Wi-Fi", platform: "AM5", chipset: "B850", detail: "Micro-ATX / DDR5 / Wi-Fi", price: 230, wifi: true, formFactor: "Micro-ATX" },
  { id: "b850-wifi", label: "B850 Gaming Wi-Fi", platform: "AM5", chipset: "B850", detail: "ATX / DDR5 / PCIe 5.0 / Wi-Fi", price: 300, wifi: true, formFactor: "ATX" },
  { id: "x870-wifi", label: "X870 Creator Wi-Fi", platform: "AM5", chipset: "X870", detail: "ATX / DDR5 / USB4 / Wi-Fi", price: 450, wifi: true, formFactor: "ATX" },
  { id: "x870e-wifi", label: "X870E Workstation Wi-Fi", platform: "AM5", chipset: "X870E", detail: "ATX / DDR5 / expanded PCIe and USB4", price: 650, wifi: true, formFactor: "ATX" },
  { id: "b860-wifi", label: "B860 Gaming Wi-Fi", platform: "LGA1851", chipset: "B860", detail: "ATX / DDR5 / Wi-Fi", price: 260, wifi: true, formFactor: "ATX" },
  { id: "z890-wifi", label: "Z890 Performance Wi-Fi", platform: "LGA1851", chipset: "Z890", detail: "ATX / DDR5 / expanded I/O", price: 390, wifi: true, formFactor: "ATX" },
];

export function getCompatibleMotherboards(cpu: PerformanceOption) {
  return motherboardOptions.filter((option) => option.platform === cpu.platform);
}

export function getMotherboardOption(id: string, cpu: PerformanceOption) {
  return getCompatibleMotherboards(cpu).find((option) => option.id === id) ?? getCompatibleMotherboards(cpu)[0];
}

export function recommendMotherboard(cpu: PerformanceOption) {
  if (cpu.platform === "LGA1851") return cpu.id === "core-ultra-7-265k" ? "z890-wifi" : "b860-wifi";
  if (["ryzen-9-9900x", "ryzen-9-9950x3d"].includes(cpu.id)) return "x870-wifi";
  return cpu.id === "ryzen-5-9600x" ? "b850m-wifi" : "b850-wifi";
}
