export type MotherboardOption = {
  id: string; label: string; platform: "AM5" | "LGA1851"; chipset: string; detail: string; wifi: boolean; formFactor: "Micro-ATX" | "ATX";
};

export const motherboardOptions: MotherboardOption[] = [
  { id: "b850m-wifi", label: "B850M Gaming Wi-Fi", platform: "AM5", chipset: "B850", detail: "Micro-ATX / DDR5 / Wi-Fi", wifi: true, formFactor: "Micro-ATX" },
  { id: "b850-wifi", label: "B850 Gaming Wi-Fi", platform: "AM5", chipset: "B850", detail: "ATX / DDR5 / PCIe 5.0 / Wi-Fi", wifi: true, formFactor: "ATX" },
  { id: "x870-wifi", label: "X870 Creator Wi-Fi", platform: "AM5", chipset: "X870", detail: "ATX / DDR5 / USB4 / Wi-Fi", wifi: true, formFactor: "ATX" },
  { id: "x870e-wifi", label: "X870E Workstation Wi-Fi", platform: "AM5", chipset: "X870E", detail: "ATX / DDR5 / expanded PCIe and USB4", wifi: true, formFactor: "ATX" },
  { id: "b860-wifi", label: "B860 Gaming Wi-Fi", platform: "LGA1851", chipset: "B860", detail: "ATX / DDR5 / Wi-Fi", wifi: true, formFactor: "ATX" },
  { id: "z890-wifi", label: "Z890 Performance Wi-Fi", platform: "LGA1851", chipset: "Z890", detail: "ATX / DDR5 / expanded I/O", wifi: true, formFactor: "ATX" },
];
