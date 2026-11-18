import strikeImage from "../assets/featured-systems/strike-5060-studio-v3.jpg";
import strikeTiImage from "../assets/featured-systems/strike-ti-studio-v3.jpg";
import stormImage from "../assets/featured-systems/storm-5070-studio-v3.jpg";
import stormTiImage from "../assets/featured-systems/storm-ti-studio-v3.jpg";
import edgeMiniImage from "../assets/minipc/edge-mini.jpg";
import edgeAiImage from "../assets/minipc/edge-ai.jpg";
import edgeProImage from "../assets/minipc/edge-pro.jpg";
import edgeMaxImage from "../assets/minipc/edge-max.jpg";
import type { PreviewSystem } from "../api/systems";

type ProductPage = { slug: string; image: string; defaultRam?: 16 | 32 };

// Presentation only. Prices, specifications and availability come from the catalogue API.
export const productPages: Record<string, ProductPage> = {
  "JON-STK-5060": { slug: "strike-5060", image: strikeImage, defaultRam: 16 },
  "JON-STK-5060TI": { slug: "strike-ti-5060-ti", image: strikeTiImage, defaultRam: 32 },
  "JON-STM-5070": { slug: "storm-5070", image: stormImage, defaultRam: 32 },
  "JON-STM-5070TI": { slug: "storm-ti-5070-ti", image: stormTiImage, defaultRam: 32 },
  "JON-EDGE-MINI": { slug: "edge-mini", image: edgeMiniImage },
  "JON-EDGE-AI": { slug: "edge-ai", image: edgeAiImage },
  "JON-EDGE-PRO": { slug: "edge-pro", image: edgeProImage },
  "JON-EDGE-MAX": { slug: "edge-max", image: edgeMaxImage },
};

export const gamingMemoryIntro = "Choose 16GB or 32GB of RAM on any gaming PC.";

export function productModelSku(sku: string) {
  const baseSku = sku.replace(/-32GB$/, "");
  return productPages[baseSku]?.defaultRam ? baseSku : sku;
}

export function systemRam(sku: string) {
  if (!productPages[productModelSku(sku)]?.defaultRam) return undefined;
  return sku.endsWith("-32GB") ? 32 : 16;
}

export function productName(name: string) {
  return name.replace(/^JON PC /, "").replace(/ · (16|32)GB RAM$/, "");
}

export function productHref(sku: string) {
  return `/products/${productPages[productModelSku(sku)]?.slug ?? sku.toLowerCase()}/`;
}

export function productModels(systems: PreviewSystem[]) {
  const models = new Map<string, PreviewSystem[]>();
  for (const system of systems) {
    const sku = productModelSku(system.sku);
    const options = models.get(sku);
    if (options) options.push(system);
    else models.set(sku, [system]);
  }
  return models;
}

export function defaultProductOption(sku: string, options: PreviewSystem[]) {
  const capacity = productPages[sku]?.defaultRam;
  return options.find(option => systemRam(option.sku) === capacity) ?? options[0];
}

export function productStartingPrice(options: PreviewSystem[]) {
  const buyable = options.filter(option => option.available && option.saleEnabled);
  const prices = (buyable.length ? buyable : options).map(option => option.priceCents);
  return {
    priceCents: Math.min(...prices),
    from: buyable.length > 1 && Math.min(...prices) !== Math.max(...prices),
    available: buyable.length > 0,
  };
}
