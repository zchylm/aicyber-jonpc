export type AssistantReply = {
  title: string;
  body: string;
  bullets?: string[];
};

export const assistantPrompts = [
  "What is the difference between RTX 5070 and RX 7800 XT?",
  "Which GPU is right for 1440p gaming?",
  "How much RAM do I need?",
  "What should I choose for an AI workstation?",
];

const replies: Array<{ keywords: string[]; reply: AssistantReply }> = [
  {
    keywords: ["5070", "7800", "difference", "compare", "comparison"],
    reply: {
      title: "RTX 5070 vs RX 7800 XT",
      body: "Both are strong 1440p GPUs, but they suit different priorities.",
      bullets: [
        "RTX 5070: stronger ray tracing, DLSS support and CUDA-based AI or creator workflows.",
        "RX 7800 XT: excellent traditional 1440p raster performance with 16GB of VRAM.",
        "Choose the RTX 5070 for ray-traced games and AI tools; choose the RX 7800 XT when VRAM and standard gaming value matter most.",
      ],
    },
  },
  {
    keywords: ["1440", "gaming", "game", "gpu"],
    reply: {
      title: "A balanced 1440p starting point",
      body: "For high-refresh 1440p gaming, JON. PC would usually start around an RTX 5070 or RX 7800 XT, paired with a modern 6 to 8-core CPU.",
      bullets: [
        "RTX 5070 is the flexible choice for ray tracing and upscaling.",
        "RX 7800 XT is a strong option for traditional rasterised games and larger VRAM capacity.",
        "The final choice should follow your games, monitor refresh rate and budget.",
      ],
    },
  },
  {
    keywords: ["ram", "memory", "how much", "32gb", "64gb", "128gb"],
    reply: {
      title: "Choose memory around your workload",
      body: "32GB DDR5 is the sensible JON. PC baseline for modern gaming, multitasking and most creative work.",
      bullets: [
        "16GB: everyday productivity and entry-level gaming.",
        "32GB: smooth gaming, streaming, multitasking and general creation.",
        "64GB or more: heavier editing, local AI, large projects and specialist production workloads.",
      ],
    },
  },
  {
    keywords: ["ai", "llm", "image generation", "workstation", "local model"],
    reply: {
      title: "Starting an AI workstation",
      body: "For local AI work, GPU memory is usually the first constraint. JON. PC would match the GPU, system RAM and power supply to the size of the models you plan to run.",
      bullets: [
        "12GB GPU memory is a practical starting point for experimentation and smaller local models.",
        "More GPU memory gives larger models and image-generation workflows more room to run locally.",
        "64GB system RAM becomes useful when datasets, development tools and models are open together.",
      ],
    },
  },
  {
    keywords: ["custom", "customize", "customise", "choose", "build"],
    reply: {
      title: "How JON. PC customisation works",
      body: "You tell us what you are building for, JON. PC recommends a balanced starting point, and you can then shape the performance, storage, platform, cooling and case style.",
      bullets: [
        "Recommendations are based on your direction, workload and budget.",
        "Only compatible motherboard, PSU, cooling and case options stay available.",
        "The live estimate updates from the recommended baseline as you make changes.",
      ],
    },
  },
];

const fallbackReply: AssistantReply = {
  title: "Let's narrow it down",
  body: "I can help explain JON. PC hardware and guide your starting configuration.",
  bullets: [
    "Try asking about GPUs, CPUs, RAM, 1440p gaming or AI workstations.",
    "You can also ask how the JON. PC custom build process works.",
  ],
};

export function getAssistantReply(question: string): AssistantReply {
  const normalized = question.toLowerCase();
  return replies.find(({ keywords }) => keywords.some((keyword) => normalized.includes(keyword)))?.reply ?? fallbackReply;
}
