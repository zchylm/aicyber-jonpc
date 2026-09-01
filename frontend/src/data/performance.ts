import type { DirectionId } from "./configurator";

export type PerformanceOption = {
  id: string;
  label: string;
  family: string;
  detail: string;
  price: number;
  platform?: "AM5" | "LGA1700";
};

export type PerformanceSelection = {
  cpuId: string;
  gpuId: string;
};

export type BudgetRange = {
  min: number;
  max: number;
  label: string;
};

export type PerformanceValidation = {
  status: "balanced" | "review";
  title: string;
  detail: string;
};

export const gpuOptions: PerformanceOption[] = [
  { id: "integrated", label: "Integrated graphics", family: "AMD / Intel", detail: "Office, display and everyday productivity", price: 0 },
  { id: "arc-b580", label: "Intel Arc B580 12GB", family: "Intel Arc", detail: "Accessible 1080p graphics and media work", price: 399 },
  { id: "rx-7800-xt", label: "AMD Radeon RX 7800 XT 16GB", family: "AMD Radeon", detail: "Strong 1440p raster performance", price: 799 },
  { id: "rtx-4060", label: "NVIDIA GeForce RTX 4060 8GB", family: "NVIDIA GeForce RTX", detail: "Efficient 1080p gaming and creator acceleration", price: 499 },
  { id: "rtx-5070", label: "NVIDIA GeForce RTX 5070 12GB", family: "NVIDIA GeForce RTX", detail: "Balanced 1440p, AI and creator performance", price: 999 },
  { id: "rtx-5080", label: "NVIDIA GeForce RTX 5080 16GB", family: "NVIDIA GeForce RTX", detail: "High-end 4K and local compute headroom", price: 1799 },
];

export const cpuOptions: PerformanceOption[] = [
  { id: "ryzen-5-7600", label: "AMD Ryzen 5 7600", family: "AMD Ryzen", detail: "Efficient 6-core AM5 starting point", price: 299, platform: "AM5" },
  { id: "core-i5-14600k", label: "Intel Core i5-14600K", family: "Intel Core", detail: "Flexible gaming and productivity performance", price: 399, platform: "LGA1700" },
  { id: "ryzen-7-7700", label: "AMD Ryzen 7 7700", family: "AMD Ryzen", detail: "Balanced 8-core performance for mixed workloads", price: 449, platform: "AM5" },
  { id: "ryzen-7-7800x3d", label: "AMD Ryzen 7 7800X3D", family: "AMD Ryzen", detail: "Gaming-focused performance with strong frame consistency", price: 599, platform: "AM5" },
  { id: "core-i7-14700k", label: "Intel Core i7-14700K", family: "Intel Core", detail: "High multi-core headroom for production workloads", price: 649, platform: "LGA1700" },
  { id: "ryzen-9-7900", label: "AMD Ryzen 9 7900", family: "AMD Ryzen", detail: "12-core capacity for AI, rendering and workstation work", price: 699, platform: "AM5" },
];

const findOption = (options: PerformanceOption[], id: string) => options.find((option) => option.id === id) ?? options[0];

export function getBudgetRange(direction: DirectionId, answers: Record<string, string>): BudgetRange {
  const budget = answers.budget ?? "";
  const match = budget.match(/\$(\d{1,3}(?:,\d{3})?)[–-]\$(\d{1,3}(?:,\d{3})?)/);
  if (match) {
    return { min: Number(match[1].replace(",", "")), max: Number(match[2].replace(",", "")), label: budget };
  }

  const plusMatch = budget.match(/\$(\d{1,3}(?:,\d{3})?)\+/);
  if (plusMatch) {
    const min = Number(plusMatch[1].replace(",", ""));
    return { min, max: min + 1800, label: `${budget} direction` };
  }

  const defaults: Record<DirectionId, BudgetRange> = {
    gaming: { min: 1500, max: 2000, label: "$1,500–$2,000" },
    ai: { min: 2000, max: 3500, label: "$2,000–$3,500" },
    creator: { min: 2000, max: 3500, label: "$2,000–$3,500" },
    workstation: { min: 3000, max: 5000, label: "$3,000–$5,000" },
    enterprise: { min: 1500, max: 2500, label: "$1,500–$2,500" },
  };
  return defaults[direction];
}

function performancePreference(direction: DirectionId, answers: Record<string, string>, gpu: PerformanceOption, cpu: PerformanceOption) {
  let score = 0;
  const resolution = answers.resolution ?? "";
  const workload = answers.workload ?? answers.creativeWork ?? answers.use ?? "";

  if (direction === "gaming") {
    if (gpu.id === "integrated") score += 800;
    if (resolution === "4K" && gpu.id !== "rtx-5080") score += 500;
    if (resolution === "1440p" && gpu.id === "integrated") score += 300;
    if (answers.games === "Competitive" && cpu.id !== "ryzen-7-7800x3d") score += 180;
  }
  if (direction === "ai" && !gpu.id.startsWith("rtx-")) score += 400;
  if ((direction === "creator" || direction === "workstation") && cpu.id === "ryzen-5-7600") score += 150;
  if ((workload === "Simulation" || workload === "Data processing" || workload === "3D and motion") && cpu.id === "ryzen-5-7600") score += 250;
  if (direction === "enterprise" && workload === "Office productivity" && gpu.id !== "integrated") score += 160;
  return score;
}

export function recommendPerformance(direction: DirectionId, answers: Record<string, string>): PerformanceSelection {
  const budgetRange = getBudgetRange(direction, answers);
  const resolution = answers.resolution ?? "";
  const workload = answers.workload ?? answers.creativeWork ?? answers.use ?? "";
  const scale = answers.scale ?? answers.reliability ?? answers.priority ?? "";

  let gpuId: string;
  let cpuId: string;

  if (direction === "gaming") {
    gpuId = resolution === "1080p" ? "rtx-4060" : resolution === "4K" ? "rtx-5080" : "rtx-5070";
    cpuId = resolution === "1080p" ? "ryzen-5-7600" : answers.games === "Competitive" ? "ryzen-7-7800x3d" : "ryzen-7-7700";
  } else if (direction === "ai") {
    gpuId = scale === "Light" ? "rtx-4060" : scale === "Heavy" ? "rtx-5080" : "rtx-5070";
    cpuId = scale === "Heavy" ? "ryzen-9-7900" : "ryzen-7-7700";
  } else if (direction === "creator") {
    gpuId = resolution === "1080p" ? "rtx-4060" : resolution === "8K" ? "rtx-5080" : "rtx-5070";
    cpuId = resolution === "8K" || workload === "3D and motion" ? "ryzen-9-7900" : "ryzen-7-7700";
  } else if (direction === "workstation") {
    gpuId = workload === "Development" && scale === "Quiet operation" ? "rtx-4060" : "rtx-5070";
    cpuId = workload === "Simulation" || workload === "Data processing" ? "ryzen-9-7900" : "core-i7-14700k";
  } else {
    gpuId = workload === "Office productivity" ? "integrated" : "rtx-4060";
    cpuId = workload === "Office productivity" ? "ryzen-5-7600" : "core-i5-14600k";
  }

  const initial = { cpuId, gpuId };
  const candidatePairs = cpuOptions.flatMap((cpu) => gpuOptions.map((gpu) => ({ cpu, gpu })));
  const target = (budgetRange.min + budgetRange.max) / 2;
  const best = candidatePairs
    .map((candidate) => {
      const price = estimateCorePrice({ cpuId: candidate.cpu.id, gpuId: candidate.gpu.id });
      const outsideDistance = price < budgetRange.min ? budgetRange.min - price : price > budgetRange.max ? price - budgetRange.max : 0;
      const budgetPenalty = outsideDistance * (price > budgetRange.max ? 4 : 0.7);
      const targetPenalty = price >= budgetRange.min && price <= budgetRange.max ? Math.abs(price - target) * 0.25 : 0;
      const preferencePenalty = performancePreference(direction, answers, candidate.gpu, candidate.cpu);
      const initialPenalty = candidate.cpu.id === initial.cpuId && candidate.gpu.id === initial.gpuId ? -50 : 0;
      return { ...candidate, score: budgetPenalty + targetPenalty + preferencePenalty + initialPenalty };
    })
    .sort((a, b) => a.score - b.score)[0];

  return { cpuId: best?.cpu.id ?? initial.cpuId, gpuId: best?.gpu.id ?? initial.gpuId };
}

export function estimateCorePrice(selection: PerformanceSelection) {
  return 899 + findOption(cpuOptions, selection.cpuId).price + findOption(gpuOptions, selection.gpuId).price;
}

export function getPerformanceOptions(selection: PerformanceSelection) {
  return { cpu: findOption(cpuOptions, selection.cpuId), gpu: findOption(gpuOptions, selection.gpuId) };
}

export function validatePerformance(direction: DirectionId, answers: Record<string, string>, selection: PerformanceSelection): PerformanceValidation {
  const { cpu, gpu } = getPerformanceOptions(selection);
  const workload = answers.workload ?? answers.creativeWork ?? answers.use ?? "";
  const resolution = answers.resolution ?? "";

  if (gpu.id === "integrated" && direction === "gaming") {
    return {
      status: "review",
      title: "Performance balance to review",
      detail: "Integrated graphics can work with this CPU, but a dedicated GPU is recommended for gaming workloads.",
    };
  }

  if (gpu.id === "integrated" && (direction === "ai" || workload === "3D and motion")) {
    return {
      status: "review",
      title: "GPU capability to review",
      detail: "This graphics choice is suitable for display and office work, but dedicated GPU acceleration is recommended here.",
    };
  }

  if (gpu.id === "rtx-5080" && ["ryzen-5-7600", "core-i5-14600k"].includes(cpu.id)) {
    return {
      status: "review",
      title: "Performance balance to review",
      detail: "This GPU can work with the selected CPU, but a higher-core CPU would better support its performance in demanding workloads.",
    };
  }

  if (direction === "gaming" && resolution === "4K" && ["integrated", "arc-b580", "rtx-4060"].includes(gpu.id)) {
    return {
      status: "review",
      title: "4K performance to review",
      detail: "This GPU can run the system, but a higher graphics tier is recommended for a stronger 4K experience.",
    };
  }

  if (direction === "ai" && !gpu.id.startsWith("rtx-")) {
    return {
      status: "review",
      title: "AI acceleration to review",
      detail: "This GPU is compatible with the CPU, but an NVIDIA RTX option is recommended for broader local AI software support.",
    };
  }

  return {
    status: "balanced",
    title: "No direct CPU / GPU conflict",
    detail: "These components can work together. Motherboard, PSU, cooling and case fit will be validated in the next hardware step.",
  };
}
