export type MiniSystem = {
  id: string;
  name: string;
  description: string;
  priceCents: number;
  recommendedFor: string;
  availability: string;
  image: string;
  detailImage?: string;
  specifications: Record<string, string>;
};

export const miniSystems: MiniSystem[] = [
  {
    id: "edge-mini",
    name: "Edge Mini",
    description: "A quiet, compact starting point for local AI tools, automation and everyday development.",
    priceCents: 129900,
    recommendedFor: "Local assistants · automation · office AI",
    availability: "Configuration and availability review",
    image: "edgeMini",
    detailImage: "edgeMiniExploded",
    specifications: {
      Processor: "AMD Ryzen 7 8845HS · 8 cores / 16 threads",
      Graphics: "Integrated Radeon 780M",
      "AI acceleration": "Ryzen AI NPU · up to 16 TOPS",
      Memory: "32GB DDR5",
      Storage: "1TB PCIe 4.0 NVMe",
      Networking: "2.5GbE · Wi-Fi 6 · Bluetooth",
      Display: "USB-C · HDMI · DisplayPort",
      OS: "Windows 11 Pro or Linux deployment",
    },
  },
  {
    id: "edge-ai",
    name: "Edge AI",
    description: "A balanced AI PC for development, content tools and accelerated local workflows.",
    priceCents: 189900,
    recommendedFor: "AI development · creator tools · edge inference",
    availability: "Configuration and availability review",
    image: "edgeAi",
    specifications: {
      Processor: "AMD Ryzen AI 9 HX 370 · 12 cores / 24 threads",
      Graphics: "Integrated Radeon 890M",
      "AI acceleration": "XDNA 2 NPU · up to 50 TOPS",
      Memory: "32GB DDR5",
      Storage: "1TB PCIe 4.0 NVMe",
      Networking: "Dual 2.5GbE · Wi-Fi 7 · Bluetooth",
      Expansion: "USB4 · multiple NVMe support",
      OS: "Windows 11 Pro or Linux deployment",
    },
  },
  {
    id: "edge-pro",
    name: "Edge Pro",
    description: "More memory and storage for heavier local models, data preparation and professional multitasking.",
    priceCents: 269900,
    recommendedFor: "Local models · data workflows · AI production",
    availability: "Configuration and availability review",
    image: "edgePro",
    specifications: {
      Processor: "AMD Ryzen AI 9 HX 370 · 12 cores / 24 threads",
      Graphics: "Integrated Radeon 890M",
      "AI acceleration": "XDNA 2 NPU · up to 50 TOPS",
      Memory: "64GB DDR5",
      Storage: "2TB PCIe 4.0 NVMe",
      Networking: "Dual 2.5GbE · Wi-Fi 7 · Bluetooth",
      Expansion: "USB4 · multi-display · additional NVMe",
      OS: "Windows 11 Pro or Linux deployment",
    },
  },
  {
    id: "edge-max",
    name: "Edge Max",
    description: "A compact local-AI workstation with unified memory for substantially larger models and datasets.",
    priceCents: 499900,
    recommendedFor: "Large local models · AI research · compact workstation",
    availability: "Engineering and availability review",
    image: "edgeMax",
    specifications: {
      Processor: "AMD Ryzen AI Max+ 395 · 16 cores / 32 threads",
      Graphics: "Integrated Radeon 8060S · 40 compute units",
      "AI acceleration": "Up to 126 combined platform TOPS",
      Memory: "128GB LPDDR5X unified memory",
      Storage: "2TB PCIe 4.0 NVMe",
      Networking: "Dual 2.5GbE · Wi-Fi 7 · Bluetooth",
      Expansion: "USB4 · multi-display · additional NVMe",
      OS: "Windows 11 Pro or Linux deployment",
    },
  },
];
