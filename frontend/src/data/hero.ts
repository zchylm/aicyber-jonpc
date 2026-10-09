import gamingImage from "../assets/hero-slides/gaming.jpg";
import aiImage from "../assets/hero-slides/ai.jpg";
import compactAiImage from "../assets/hero-slides/compact-ai.jpg";
import customImage from "../assets/hero-slides/custom.jpg";

export const heroSlides = [
  {
    id: "gaming",
    kicker: "01 / Gaming",
    title: "Built to play.",
    description: "Gaming PCs tuned for high frame rates, fast response and immersive play.",
    cta: { label: "View gaming systems", href: "#systems" },
    image: gamingImage,
    alt: "A player at a multi-monitor gaming PC setup",
  },
  {
    id: "compact-ai",
    kicker: "02 / Compact AI",
    title: "Built for\nAI.",
    description: "Compact local AI systems, ready for your desk.",
    cta: { label: "View compact systems", href: "#compact-ai-systems" },
    image: compactAiImage,
    alt: "A compact JON.PC AI system in a creative workstation",
  },
  {
    id: "ai-infrastructure",
    kicker: "03 / AI Infrastructure",
    title: "Built to scale.",
    description: "From individual GPU nodes to large-scale AI infrastructure for business.",
    cta: { label: "Explore AI infrastructure", href: "#ai-infrastructure" },
    image: aiImage,
    alt: "Close-up of high-performance compute hardware",
  },
  {
    id: "custom",
    kicker: "04 / Custom",
    title: "Built around you.",
    description: "Choose the parts, look and performance that fit your way of working.",
    cta: { label: "Customize your PC", href: "#build" },
    image: customImage,
    alt: "Detailed close-up of a custom graphics card and cooling system",
  },
] as const;
