import type { PerformanceOption } from "./performance";

export type MotherboardOption = {
  id: string;
  label: string;
  platform: "AM5" | "LGA1700";
  chipset: string;
  detail: string;
  price: number;
  wifi: boolean;
  formFactor: "Micro-ATX" | "ATX";
};

export const motherboardOptions: MotherboardOption[] = [
  { id: "b650m-no-wifi", label: "B650M Gaming", platform: "AM5", chipset: "B650", detail: "Micro-ATX / DDR5 / No Wi-Fi", price: 0, wifi: false, formFactor: "Micro-ATX" },
  { id: "b650-wifi", label: "B650 Gaming Wi-Fi", platform: "AM5", chipset: "B650", detail: "ATX / DDR5 / Wi-Fi 6E", price: 80, wifi: true, formFactor: "ATX" },
  { id: "b850-wifi", label: "B850 Gaming Wi-Fi", platform: "AM5", chipset: "B850", detail: "ATX / DDR5 / PCIe 5.0 / Wi-Fi", price: 180, wifi: true, formFactor: "ATX" },
  { id: "x870-wifi", label: "X870 Creator Wi-Fi", platform: "AM5", chipset: "X870", detail: "ATX / DDR5 / USB4 / Wi-Fi", price: 300, wifi: true, formFactor: "ATX" },
  { id: "x870e-wifi", label: "X870E Workstation Wi-Fi", platform: "AM5", chipset: "X870E", detail: "ATX / DDR5 / PCIe 5.0 / USB4", price: 500, wifi: true, formFactor: "ATX" },
  { id: "b760-no-wifi", label: "B760 Gaming", platform: "LGA1700", chipset: "B760", detail: "ATX / DDR5 / No Wi-Fi", price: 0, wifi: false, formFactor: "ATX" },
  { id: "b760-wifi", label: "B760 Gaming Wi-Fi", platform: "LGA1700", chipset: "B760", detail: "ATX / DDR5 / Wi-Fi 6E", price: 80, wifi: true, formFactor: "ATX" },
  { id: "z790-wifi", label: "Z790 Performance Wi-Fi", platform: "LGA1700", chipset: "Z790", detail: "ATX / DDR5 / Expanded I/O", price: 260, wifi: true, formFactor: "ATX" },
];

export function getCompatibleMotherboards(cpu: PerformanceOption) {
  return motherboardOptions.filter((option) => option.platform === cpu.platform);
}

export function getMotherboardOption(id: string, cpu: PerformanceOption) {
  return getCompatibleMotherboards(cpu).find((option) => option.id === id) ?? getCompatibleMotherboards(cpu)[0];
}

export function recommendMotherboard(cpu: PerformanceOption) {
  if (cpu.platform === "LGA1700") return "b760-wifi";
  return "b850-wifi";
}
