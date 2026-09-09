import { presetBuilds } from "./presetBuilds";

export const focusAreas = [
  { label: "Gaming", id: "gaming" },
  { label: "AI", id: "ai" },
  { label: "Creator", id: "creator" },
  { label: "Workstation", id: "workstation" },
  { label: "Enterprise", id: "enterprise" },
];

export const navigationItems = [
  { label: "Systems", href: "#systems" },
  { label: "Rewards", href: "#queue-rewards" },
  { label: "Customize", href: "#build" },
  { label: "Support", href: "#support" },
];

export const systemRecommendations = {
  gaming: {
    kicker: "01 / Gaming",
    title: "Built for the frame you want.",
    description: "Balanced systems for fast, fluid play, from everyday 1080p to high-refresh 1440p gaming.",
    builds: [
      {
        ...presetBuilds["core-gaming"],
        image: "gaming01",
      },
      {
        ...presetBuilds["performance-gaming"],
        image: "gaming02",
      },
      {
        name: "Custom Gaming",
        direction: "gaming",
        tier: "You choose, we balance the build",
        price: "Configure for price",
        image: "gaming03",
        specs: ["Choose your GPU", "Budget-led options", "RGB or clean build", "Local support"],
      },
    ],
  },
  ai: {
    kicker: "02 / AI",
    title: "More memory for bigger ideas.",
    description: "Quiet, upgrade-ready systems for local models, data workflows and AI-assisted production.",
    builds: [
      {
        ...presetBuilds["ai-starter"],
        image: "ai01",
      },
      {
        ...presetBuilds["ai-creator"],
        image: "ai02",
      },
      {
        name: "AI Custom",
        direction: "ai",
        tier: "You choose the workload target",
        price: "Configure for price",
        image: "ai03",
        specs: ["Choose your GPU", "Memory expansion", "Workflow review", "Local support"],
      },
    ],
  },
  creator: {
    kicker: "03 / Creator",
    title: "Make more room to create.",
    description: "Responsive systems for editing, design, 3D work and the applications that keep your ideas moving.",
    builds: [
      {
        ...presetBuilds["creator-core"],
        image: "creator01",
      },
      {
        ...presetBuilds["creator-pro"],
        image: "creator02",
      },
      {
        name: "Studio Custom",
        direction: "creator",
        tier: "Your apps, your budget, your setup",
        price: "Configure for price",
        image: "creator03",
        specs: ["Application matched", "Budget-led options", "Quiet acoustics", "Upgrade path"],
      },
    ],
  },
  workstation: {
    kicker: "04 / Workstation",
    title: "Serious power, intelligently arranged.",
    description: "Stable, expandable systems designed around demanding professional workloads and long sessions.",
    builds: [
      {
        ...presetBuilds["workstation-core"],
        image: "workstation01",
      },
      {
        ...presetBuilds["workstation-pro"],
        image: "workstation02",
      },
      {
        name: "Custom Workstation",
        direction: "workstation",
        tier: "Specified around your priorities",
        price: "Configure for price",
        image: "workstation03",
        specs: ["Choose your baseline", "Component matched", "Thermal tuning", "Local support"],
      },
    ],
  },
  enterprise: {
    kicker: "05 / Enterprise",
    title: "Reliable systems for real work.",
    description: "Purpose-built desktop systems for teams that need repeatable performance, support and a clear upgrade path.",
    builds: [
      {
        ...presetBuilds["team-core"],
        image: "custom01",
      },
      {
        ...presetBuilds["team-performance"],
        image: "custom02",
      },
      {
        name: "Business Custom",
        direction: "enterprise",
        tier: "Choose the right level for your team",
        price: "Configure for price",
        image: "custom03",
        specs: ["Choose your baseline", "Deployment planning", "Support options", "Upgrade path"],
      },
    ],
  },
} as const;

const componentRows = (rows: Array<[string, string, string, string]>) => rows.map(([label, value, role, reason]) => ({ label, value, role, reason }));

export const systemDetails = {
  "Custom Gaming": {
    summary: "Choose the parts that matter to you. We balance the rest around your budget.",
    performance: "Configured around your games, display and budget",
    components: componentRows([
      ["CPU", "Ryzen 5 / Ryzen 7 / Core i5 options", "The processor", "We match the CPU to your games, target frame rate and total budget."],
      ["GPU", "RTX 3060 / 4060 / 5060 and beyond", "Graphics and AI", "You choose the graphics level; we explain the trade-offs for resolution and settings."],
      ["RAM", "16GB or 32GB DDR5", "Working memory", "Start with what your games need and add more later when your workflow grows."],
      ["Storage", "1TB NVMe, expandable", "Long-term storage", "Choose the capacity that fits your library without paying for unused space."],
      ["Motherboard", "Compatible AM5 or LGA platform", "The connection platform", "Selected to keep your chosen parts compatible and future upgrades realistic."],
      ["PSU", "550W-850W, efficiency matched", "Power delivery", "Sized to your actual GPU choice instead of forcing a high-end power budget."],
      ["CPU Cooler", "Air or liquid, workload matched", "CPU thermals", "Chosen around the selected CPU, noise preference and case size."],
      ["Case", "Your choice of airflow chassis", "The chassis", "You choose the look and footprint; we check clearance and airflow."],
      ["Fans", "PWM airflow setup", "System airflow", "Tuned to keep the selected configuration cool without unnecessary noise."],
    ]),
  },
  "AI Custom": {
    summary: "Choose the AI capability you need, from an affordable local setup to a larger model workstation.",
    performance: "Specified for your model, tools and budget",
    components: componentRows([
      ["CPU", "Ryzen 5 / Ryzen 7 / Ryzen 9 options", "The processor", "We balance preprocessing and orchestration needs against the budget left for the GPU."],
      ["GPU", "RTX 4060 / 5070 / higher VRAM options", "Accelerated compute", "You choose the VRAM target; we explain which local models fit within it."],
      ["RAM", "32GB-128GB DDR5", "Working memory", "Scale memory to datasets, containers and the number of tools you run together."],
      ["Storage", "1TB-4TB NVMe architecture", "Long-term storage", "Choose between a compact starter library and larger model storage."],
      ["Motherboard", "Expansion-ready platform", "The connection platform", "Chosen around the GPU, memory capacity and future expansion you select."],
      ["PSU", "650W-1200W, efficiency matched", "Power delivery", "Calculated from the GPU you choose rather than assuming a maximum build."],
      ["CPU Cooler", "Workload-matched cooling", "CPU thermals", "Thermals are planned for your actual sustained workload."],
      ["Case", "Compact to large serviceable case", "The chassis", "You choose the footprint; we check access, airflow and component clearance."],
      ["Fans", "Pressure-balanced PWM setup", "System airflow", "Configured to move heat reliably through the chosen workload."],
    ]),
  },
  "Studio Custom": {
    summary: "Choose the applications and budget first. We build a system that supports the way you create.",
    performance: "Application-matched creative workflow and budget",
    components: componentRows([
      ["CPU", "Ryzen 7-Ryzen 9 class", "The processor", "Selected around your export, simulation and multitasking requirements."],
      ["GPU", "Integrated / RTX 4060 / RTX 5070 options", "Graphics and compute", "You choose the acceleration level based on your renderer, timeline and display."],
      ["RAM", "32GB-128GB DDR5", "Working memory", "Scaled to project size and the applications you run together."],
      ["Storage", "1TB-4TB Gen4 NVMe", "Long-term storage", "Structured around active projects, cache and archive needs."],
      ["Motherboard", "Expansion-ready platform", "The connection platform", "Chosen for your storage, display and peripheral requirements."],
      ["PSU", "650W-1000W, efficiency matched", "Power delivery", "Sized for the final components with practical headroom."],
      ["CPU Cooler", "Acoustic preference matched", "CPU thermals", "Cooling is selected around sustained load and studio noise levels."],
      ["Case", "Studio-fit chassis", "The chassis", "Balances desk footprint, appearance and service access."],
      ["Fans", "Tuned PWM airflow", "System airflow", "Keeps the system steady without distracting fan noise."],
    ]),
  },
  "Custom Workstation": {
    summary: "Start with the work you need to do and the budget you want to keep. We make the parts work together.",
    performance: "Specified for your professional workload and budget",
    components: componentRows([
      ["CPU", "Ryzen 9 or Intel Core Ultra class", "The processor", "Chosen from application requirements and sustained compute behaviour."],
      ["GPU", "Integrated / RTX 4060 / professional GPU options", "Graphics and compute", "Choose the level of acceleration your viewport, simulation or visualisation actually needs."],
      ["RAM", "64GB-256GB DDR5", "Working memory", "Scaled to project size, virtual machines and professional datasets."],
      ["Storage", "2TB-8TB NVMe architecture", "Long-term storage", "Designed around active files, scratch space and backup strategy."],
      ["Motherboard", "Expansion-first workstation board", "The connection platform", "PCIe lanes, memory capacity and I/O are planned before selection."],
      ["PSU", "850W-1200W, efficiency matched", "Power delivery", "Calculated for sustained load and future expansion."],
      ["CPU Cooler", "Thermal design matched", "CPU thermals", "Built for predictable temperatures in the target environment."],
      ["Case", "Serviceable professional chassis", "The chassis", "Prioritises access, reliability, acoustics and physical compatibility."],
      ["Fans", "Redundant PWM airflow", "System airflow", "Keeps the system stable and easier to maintain."],
    ]),
  },
  "Business Custom": {
    summary: "Choose the right level for each team member, then let JON. PC make the rollout consistent.",
    performance: "Specified for your business deployment and budget",
    components: componentRows([
      ["CPU", "Business workload matched", "The processor", "Selected for application compatibility, longevity and support requirements."],
      ["GPU", "Integrated or dedicated GPU", "Graphics and compute", "Specified only where the workflow benefits from dedicated acceleration."],
      ["RAM", "16GB-64GB DDR5", "Working memory", "Scaled to user role, software and multi-year service expectations."],
      ["Storage", "1TB-2TB NVMe", "Long-term storage", "Fast local storage with a clear data and backup strategy."],
      ["Motherboard", "Standardised deployment platform", "The connection platform", "Repeatability and service access are prioritised across the fleet."],
      ["PSU", "Efficiency matched", "Power delivery", "Sized for the final build and expected duty cycle."],
      ["CPU Cooler", "Low-maintenance cooling", "CPU thermals", "Keeps office systems consistent and easy to support."],
      ["Case", "Rollout-ready serviceable case", "The chassis", "Chosen for consistent assembly, access and desk compatibility."],
      ["Fans", "Quiet PWM airflow", "System airflow", "Supports reliable operation without adding workplace noise."],
    ]),
  },
} as const;
