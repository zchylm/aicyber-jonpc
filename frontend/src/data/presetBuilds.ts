import type { ConfiguratorQuoteRequest } from "../api/configurator";
import type { DirectionId } from "./configurator";
import { caseOptions, getCompatibleCases, type CaseOption } from "./case";
import { coolingOptions, getCompatibleCooling } from "./cooling";
import { memoryOptions } from "./memory";
import { getCompatibleMotherboards, motherboardOptions } from "./motherboard";
import { cpuOptions, estimateCorePrice, gpuOptions } from "./performance";
import { getCompatiblePsus, psuOptions } from "./psu";
import { storageOptions } from "./storage";

export type PresetBuildId =
  | "core-gaming"
  | "performance-gaming"
  | "ai-starter"
  | "ai-creator"
  | "creator-core"
  | "creator-pro"
  | "workstation-core"
  | "workstation-pro"
  | "team-core"
  | "team-performance";

type PresetSelection = {
  cpuId: string;
  gpuId: string;
  memoryId: string;
  storageId: string;
  motherboardId: string;
  psuId: string;
  caseId: CaseOption["id"];
  coolingId: string;
  caseColorId: string;
};

type PresetDefinition = {
  id: PresetBuildId;
  name: string;
  tier: string;
  direction: DirectionId;
  summary: string;
  performance: string;
  answers: Record<string, string>;
  selection: PresetSelection;
};

export type PresetComponent = {
  label: string;
  value: string;
  role: string;
  reason: string;
};

export type PresetBuild = PresetDefinition & {
  price: number;
  priceLabel: string;
  specs: string[];
  components: PresetComponent[];
  configuration: ConfiguratorQuoteRequest;
};

const definitions: PresetDefinition[] = [
  {
    id: "core-gaming",
    name: "Core Gaming",
    tier: "1080p performance",
    direction: "gaming",
    summary: "A focused 1080p gaming system with a clean upgrade path.",
    performance: "1080p high settings / esports high refresh",
    answers: { resolution: "1080p", games: "Mixed", budget: "$1,500–$2,000" },
    selection: { cpuId: "ryzen-5-7600", gpuId: "rtx-4060", memoryId: "16gb", storageId: "1tb", motherboardId: "b650m-no-wifi", psuId: "650-bronze", caseId: "compact", coolingId: "tower-air", caseColorId: "white" },
  },
  {
    id: "performance-gaming",
    name: "Performance Gaming",
    tier: "1440p ready",
    direction: "gaming",
    summary: "A balanced 1440p system for high-refresh play and streaming.",
    performance: "1440p high settings / high-refresh gaming",
    answers: { resolution: "1440p", games: "Mixed", budget: "$2,000–$3,000" },
    selection: { cpuId: "ryzen-7-7700", gpuId: "rtx-5070", memoryId: "32gb", storageId: "2tb", motherboardId: "b850-wifi", psuId: "750-gold", caseId: "mid", coolingId: "240-liquid", caseColorId: "black" },
  },
  {
    id: "ai-starter",
    name: "AI Starter",
    tier: "Local tools and inference",
    direction: "ai",
    summary: "A practical entry point for local AI tools, automation and inference.",
    performance: "Local inference / AI-assisted productivity",
    answers: { workload: "AI development", scale: "Medium", budget: "$2,000–$3,500" },
    selection: { cpuId: "ryzen-7-7700", gpuId: "rtx-5070", memoryId: "32gb", storageId: "2tb", motherboardId: "b850-wifi", psuId: "850-gold", caseId: "mid", coolingId: "240-liquid", caseColorId: "black" },
  },
  {
    id: "ai-creator",
    name: "AI Creator",
    tier: "Larger local workloads",
    direction: "ai",
    summary: "More memory and storage headroom for larger local models and creative workflows.",
    performance: "Larger local models / 4K creation / GPU compute",
    answers: { workload: "Image generation", scale: "Heavy", budget: "$3,500+" },
    selection: { cpuId: "ryzen-9-7900", gpuId: "rtx-5080", memoryId: "64gb", storageId: "4tb", motherboardId: "x870-wifi", psuId: "1000-platinum", caseId: "full", coolingId: "360-liquid", caseColorId: "black" },
  },
  {
    id: "creator-core",
    name: "Creator Core",
    tier: "Photo and video workflow",
    direction: "creator",
    summary: "A responsive editing and design system for everyday creative production.",
    performance: "Photo / 1080p-4K editing / design apps",
    answers: { creativeWork: "Photo and design", resolution: "4K", budget: "$2,000–$3,500" },
    selection: { cpuId: "ryzen-7-7700", gpuId: "rtx-5070", memoryId: "32gb", storageId: "2tb", motherboardId: "b650-wifi", psuId: "750-gold", caseId: "mid", coolingId: "tower-air", caseColorId: "white" },
  },
  {
    id: "creator-pro",
    name: "Creator Pro",
    tier: "4K editing and 3D",
    direction: "creator",
    summary: "A higher-headroom creator system for 4K, 3D and demanding production tools.",
    performance: "4K-8K editing / 3D / motion graphics",
    answers: { creativeWork: "3D and motion", resolution: "8K", budget: "$3,500+" },
    selection: { cpuId: "ryzen-9-7900", gpuId: "rtx-5080", memoryId: "64gb", storageId: "4tb", motherboardId: "x870-wifi", psuId: "1000-platinum", caseId: "full", coolingId: "360-liquid", caseColorId: "silver" },
  },
  {
    id: "workstation-core",
    name: "Workstation Core",
    tier: "Professional productivity",
    direction: "workstation",
    summary: "A dependable professional desktop for demanding daily workloads.",
    performance: "Professional productivity / data / development",
    answers: { workload: "Development", reliability: "Maximum performance", budget: "$2,000–$3,000" },
    selection: { cpuId: "ryzen-9-7900", gpuId: "rtx-5070", memoryId: "64gb", storageId: "2tb", motherboardId: "x870-wifi", psuId: "850-gold", caseId: "mid", coolingId: "240-liquid", caseColorId: "black" },
  },
  {
    id: "workstation-pro",
    name: "Workstation Pro",
    tier: "Heavy production workloads",
    direction: "workstation",
    summary: "A high-capacity workstation for technical production and heavy computation.",
    performance: "Simulation / 3D / engineering / large datasets",
    answers: { workload: "Simulation", reliability: "Maximum performance", budget: "$3,000–$5,000" },
    selection: { cpuId: "ryzen-9-7900", gpuId: "rtx-5080", memoryId: "128gb", storageId: "4tb", motherboardId: "x870e-wifi", psuId: "1000-platinum", caseId: "full", coolingId: "360-liquid", caseColorId: "black" },
  },
  {
    id: "team-core",
    name: "Team Core",
    tier: "Everyday business power",
    direction: "enterprise",
    summary: "A reliable everyday system for teams that need repeatable deployment.",
    performance: "Business productivity / office workloads",
    answers: { use: "Office productivity", priority: "Standardised builds", budget: "$1,000–$1,500" },
    selection: { cpuId: "ryzen-5-7600", gpuId: "integrated", memoryId: "16gb", storageId: "1tb", motherboardId: "b650m-no-wifi", psuId: "550-bronze", caseId: "compact", coolingId: "tower-air", caseColorId: "white" },
  },
  {
    id: "team-performance",
    name: "Team Performance",
    tier: "Data and production teams",
    direction: "enterprise",
    summary: "A stronger team system for data, production and specialist business software.",
    performance: "Data workflows / production teams / development",
    answers: { use: "Production", priority: "Performance", budget: "$1,500–$2,500" },
    selection: { cpuId: "ryzen-7-7700", gpuId: "rtx-4060", memoryId: "32gb", storageId: "2tb", motherboardId: "b650-wifi", psuId: "650-silver", caseId: "mid", coolingId: "tower-air", caseColorId: "black" },
  },
];

function required<T extends { id: string }>(options: T[], id: string, label: string) {
  const option = options.find((item) => item.id === id);
  if (!option) throw new Error(`Unknown ${label} option in preset build: ${id}`);
  return option;
}

function buildPreset(definition: PresetDefinition): PresetBuild {
  const { selection } = definition;
  const cpu = required(cpuOptions, selection.cpuId, "CPU");
  const gpu = required(gpuOptions, selection.gpuId, "GPU");
  const memory = required(memoryOptions, selection.memoryId, "memory");
  const storage = required(storageOptions, selection.storageId, "storage");
  const motherboard = required(motherboardOptions, selection.motherboardId, "motherboard");
  const psu = required(psuOptions, selection.psuId, "PSU");
  const buildCase = required(caseOptions, selection.caseId, "case");
  const cooling = required(coolingOptions, selection.coolingId, "cooling");
  const caseColor = required([...buildCase.colors, ...buildCase.moreColors], selection.caseColorId, "case colour");
  if (!getCompatibleMotherboards(cpu).some((option) => option.id === motherboard.id)) {
    throw new Error(`Motherboard is incompatible with CPU in preset build: ${definition.id}`);
  }
  if (!getCompatiblePsus(gpu).some((option) => option.id === psu.id)) {
    throw new Error(`PSU is incompatible with GPU in preset build: ${definition.id}`);
  }
  if (!getCompatibleCases(motherboard).some((option) => option.id === buildCase.id)) {
    throw new Error(`Case is incompatible with motherboard in preset build: ${definition.id}`);
  }
  if (!getCompatibleCooling(cpu, buildCase.id).some((option) => option.id === cooling.id)) {
    throw new Error(`Cooling is incompatible with CPU or case in preset build: ${definition.id}`);
  }
  const price = estimateCorePrice({ cpuId: cpu.id, gpuId: gpu.id });
  const configuration: ConfiguratorQuoteRequest = {
    direction: definition.direction,
    answers: definition.answers,
    recommendedCpuId: cpu.id,
    recommendedGpuId: gpu.id,
    recommendedMemoryId: memory.id,
    recommendedStorageId: storage.id,
    recommendedMotherboardId: motherboard.id,
    recommendedPsuId: psu.id,
    recommendedCaseId: buildCase.id,
    recommendedCoolingId: cooling.id,
    cpuId: cpu.id,
    gpuId: gpu.id,
    memoryId: memory.id,
    storageId: storage.id,
    motherboardId: motherboard.id,
    psuId: psu.id,
    caseId: buildCase.id,
    coolingId: cooling.id,
    caseColorId: caseColor.id,
  };

  return {
    ...definition,
    price,
    priceLabel: `$${price.toLocaleString("en-AU")} AUD`,
    specs: [cpu.label.replace("AMD ", "").replace("NVIDIA GeForce ", ""), gpu.label.replace("NVIDIA GeForce ", ""), memory.label, storage.label],
    components: [
      { label: "CPU", value: cpu.label, role: "The processor", reason: cpu.detail },
      { label: "GPU", value: gpu.label, role: "Graphics and compute", reason: gpu.detail },
      { label: "RAM", value: memory.label, role: "Working memory", reason: memory.detail },
      { label: "Storage", value: storage.label, role: "Long-term storage", reason: storage.detail },
      { label: "Motherboard", value: motherboard.label, role: "The connection platform", reason: motherboard.detail },
      { label: "PSU", value: psu.label, role: "Power delivery", reason: `${psu.detail} / ${psu.modular}` },
      { label: "CPU Cooler", value: cooling.label, role: "CPU thermals", reason: cooling.detail },
      { label: "Case", value: `${buildCase.label} / ${caseColor.label}`, role: "The chassis", reason: buildCase.detail },
    ],
    configuration,
  };
}

export const presetBuilds = Object.fromEntries(
  definitions.map((definition) => [definition.id, buildPreset(definition)]),
) as Record<PresetBuildId, PresetBuild>;

export function getPresetBuild(id: PresetBuildId) {
  return presetBuilds[id];
}
