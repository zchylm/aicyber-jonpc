import strikeImage from "../assets/featured-systems/strike-5060-studio-v3.jpg";
import strikeTiImage from "../assets/featured-systems/strike-ti-studio-v3.jpg";
import stormImage from "../assets/featured-systems/storm-5070-studio-v3.jpg";
import stormTiImage from "../assets/featured-systems/storm-ti-studio-v3.jpg";
import edgeMiniImage from "../assets/minipc/edge-mini.jpg";
import edgeAiImage from "../assets/minipc/edge-ai.jpg";
import edgeProImage from "../assets/minipc/edge-pro.jpg";
import edgeMaxImage from "../assets/minipc/edge-max.jpg";

// Presentation only. Prices, specifications and availability come from the catalogue API.
export const productPages: Record<string, { slug: string; image: string }> = {
  "JON-STK-5060": { slug: "strike-5060", image: strikeImage },
  "JON-STK-5060TI": { slug: "strike-ti-5060-ti", image: strikeTiImage },
  "JON-STM-5070": { slug: "storm-5070", image: stormImage },
  "JON-STM-5070TI": { slug: "storm-ti-5070-ti", image: stormTiImage },
  "JON-EDGE-MINI": { slug: "edge-mini", image: edgeMiniImage },
  "JON-EDGE-AI": { slug: "edge-ai", image: edgeAiImage },
  "JON-EDGE-PRO": { slug: "edge-pro", image: edgeProImage },
  "JON-EDGE-MAX": { slug: "edge-max", image: edgeMaxImage },
};

export function productHref(sku: string) {
  return `/products/${productPages[sku]?.slug ?? sku.toLowerCase()}/`;
}
