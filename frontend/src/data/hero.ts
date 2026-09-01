import gamingImage from "../assets/hero-slides/gaming.jpg";
import aiImage from "../assets/hero-slides/ai.jpg";
import creatorImage from "../assets/hero-slides/creator.jpg";
import workstationImage from "../assets/hero-slides/workstation.jpg";
import customImage from "../assets/hero-slides/custom.jpg";

export const heroSlides = [
  {
    id: "gaming",
    kicker: "01 / Gaming",
    title: "Built to play.",
    description: "Gaming PCs tuned for high frame rates, fast response and immersive play.",
    image: gamingImage,
    alt: "A player at a multi-monitor gaming PC setup",
  },
  {
    id: "ai",
    kicker: "02 / AI",
    title: "Built to think.",
    description: "GPU-powered systems for local AI, experimentation and development.",
    image: aiImage,
    alt: "Close-up of a powerful graphics card and liquid-cooled PC interior",
  },
  {
    id: "creator",
    kicker: "03 / Creator",
    title: "Built to create.",
    description: "Balanced systems for editing, motion, design and content production.",
    image: creatorImage,
    alt: "A creator desk with a display, peripherals and an RGB desktop PC",
  },
  {
    id: "workstation",
    kicker: "04 / Workstation",
    title: "Built to work.",
    description: "Reliable desktop performance for demanding professional workflows.",
    image: workstationImage,
    alt: "A minimal blue-lit workstation with a desktop PC",
  },
  {
    id: "custom",
    kicker: "05 / Custom",
    title: "Built around you.",
    description: "Choose the parts, look and performance that fit your way of working.",
    image: customImage,
    alt: "Detailed close-up of a custom graphics card and cooling system",
  },
] as const;
