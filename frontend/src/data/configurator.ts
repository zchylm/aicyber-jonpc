export type DirectionId = "gaming" | "ai" | "creator" | "workstation" | "enterprise";

export type ConfiguratorOption = {
  label: string;
  detail?: string;
  recommended?: boolean;
};

export type ConfiguratorQuestion = {
  id: string;
  label: string;
  description: string;
  options: ConfiguratorOption[];
};

export const configuratorDirections: Array<{ id: DirectionId; label: string; detail: string }> = [
  { id: "gaming", label: "Gaming", detail: "Frames, speed and play" },
  { id: "ai", label: "AI", detail: "Local models and compute" },
  { id: "creator", label: "Creator", detail: "Edit, design and render" },
  { id: "workstation", label: "Workstation", detail: "Professional workloads" },
  { id: "enterprise", label: "Enterprise", detail: "Reliable team systems" },
];

export const configuratorQuestions: Record<DirectionId, ConfiguratorQuestion[]> = {
  gaming: [
    { id: "resolution", label: "Resolution", description: "What will you play at most?", options: [{ label: "1080p", detail: "Fast and accessible" }, { label: "1440p", detail: "Balanced detail", recommended: true }, { label: "4K", detail: "Maximum clarity" }] },
    { id: "games", label: "Games", description: "What kind of games do you play?", options: [{ label: "Competitive", detail: "High refresh and low latency" }, { label: "AAA", detail: "Detail and visual quality" }, { label: "Mixed", detail: "A flexible balance", recommended: true }] },
    { id: "budget", label: "Budget", description: "Where should we start?", options: [{ label: "$1,000–$1,500", detail: "Focused essentials" }, { label: "$1,500–$2,000", detail: "Balanced performance", recommended: true }, { label: "$2,000–$3,000", detail: "More headroom" }, { label: "$3,000+", detail: "No-compromise direction" }] },
  ],
  ai: [
    { id: "workload", label: "Workload", description: "What do you want to run locally?", options: [{ label: "Local LLM", detail: "Text and code models" }, { label: "Image generation", detail: "Diffusion and visual tools" }, { label: "AI development", detail: "Frameworks and experiments", recommended: true }, { label: "General experimentation", detail: "A flexible starting point" }] },
    { id: "scale", label: "Model size", description: "How much compute do you expect to need?", options: [{ label: "Light", detail: "Small local tools" }, { label: "Medium", detail: "A broader model range", recommended: true }, { label: "Heavy", detail: "VRAM and memory first" }] },
    { id: "budget", label: "Budget", description: "Where should we start?", options: [{ label: "$1,500–$2,000", detail: "Practical local setup" }, { label: "$2,000–$3,500", detail: "More VRAM headroom", recommended: true }, { label: "$3,500+", detail: "Serious local compute" }] },
  ],
  creator: [
    { id: "creativeWork", label: "Creative work", description: "What do you make most?", options: [{ label: "Photo and design", detail: "Responsive 2D workflows" }, { label: "Video editing", detail: "Timelines and exports", recommended: true }, { label: "3D and motion", detail: "Viewport and rendering" }] },
    { id: "resolution", label: "Resolution", description: "What will you create at?", options: [{ label: "1080p", detail: "Fast project workflow" }, { label: "4K", detail: "Modern creator standard", recommended: true }, { label: "8K", detail: "Large project headroom" }] },
    { id: "budget", label: "Budget", description: "Where should we start?", options: [{ label: "$1,500–$2,000", detail: "Focused creator system" }, { label: "$2,000–$3,500", detail: "Balanced production", recommended: true }, { label: "$3,500+", detail: "High-end studio direction" }] },
  ],
  workstation: [
    { id: "workload", label: "Workload", description: "What needs the most power?", options: [{ label: "Development", detail: "Compilers and containers" }, { label: "Engineering", detail: "CAD and technical tools" }, { label: "Simulation", detail: "Long compute sessions" }, { label: "Data processing", detail: "Large files and datasets", recommended: true }] },
    { id: "reliability", label: "Priority", description: "What matters most day to day?", options: [{ label: "Quiet operation", detail: "A calmer desk" }, { label: "Maximum performance", detail: "Time saved in heavy work", recommended: true }, { label: "Expandability", detail: "Room to grow" }] },
    { id: "budget", label: "Budget", description: "Where should we start?", options: [{ label: "$2,000–$3,000", detail: "Professional baseline" }, { label: "$3,000–$5,000", detail: "High-capacity system", recommended: true }, { label: "$5,000+", detail: "Specialist workstation" }] },
  ],
  enterprise: [
    { id: "use", label: "Team use", description: "Who is this system for?", options: [{ label: "Office productivity", detail: "Everyday business tools" }, { label: "Development", detail: "Code and technical work" }, { label: "Production", detail: "Creative or data teams", recommended: true }] },
    { id: "priority", label: "Priority", description: "What should the rollout optimise?", options: [{ label: "Standardised builds", detail: "Simple fleet support", recommended: true }, { label: "Performance", detail: "More capability per user" }, { label: "Long-term support", detail: "A clear service path" }] },
    { id: "budget", label: "Budget per system", description: "Where should we start?", options: [{ label: "$1,000–$1,500", detail: "Efficient baseline" }, { label: "$1,500–$2,500", detail: "Balanced team system", recommended: true }, { label: "$2,500+", detail: "Specialist roles" }] },
  ],
};
