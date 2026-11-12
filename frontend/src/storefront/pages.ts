import type { PreviewSystem } from "../api/systems";
import { productHref, productPages } from "../data/productPages";

export const siteOrigin = "https://jonpc.com.au";
export type StorefrontPage = { kind: "product"; sku: string } | { kind: "gaming" | "mini" | "custom" | "ai" };
export type StorefrontSnapshot = { page: StorefrontPage; systems: PreviewSystem[] };

export const collections = {
  gaming: { path: "/gaming-pcs/", label: "Gaming PCs", eyebrow: "Strike & Storm series", description: "Shop JON.PC gaming desktops in West Melbourne. Compare the Strike and Storm range by graphics, specifications and price, with online ordering and local pickup." },
  mini: { path: "/mini-pcs/", label: "Mini PCs", eyebrow: "Edge series", description: "Explore JON.PC Edge Mini PCs for work and local AI. Compare processors, memory and storage, with online ordering and pickup in West Melbourne." },
  custom: { path: "/custom-pcs/", label: "Custom PCs", eyebrow: "Make it yours", description: "Configure a custom PC with JON.PC in West Melbourne. Choose your parts and budget, then receive an availability check and a reviewed final quote." },
  ai: { path: "/ai-solutions/", label: "AI Solutions", eyebrow: "Compute with a purpose", description: "Plan local AI compute with JON.PC in West Melbourne, from compact PCs to tower and rack infrastructure. Submit your requirements for engineering review and a tailored quote." },
};

export function pageProduct(page: StorefrontPage, systems: PreviewSystem[]) {
  return page.kind === "product" ? systems.find(system => system.sku === page.sku) : undefined;
}

export function pageMetadata(page: StorefrontPage, systems: PreviewSystem[]) {
  if (page.kind !== "product") {
    const collection = collections[page.kind];
    return { path: collection.path, title: `${collection.label} Melbourne | JON.PC`, description: collection.description, image: "/social/jonpc-share.jpg", label: collection.label };
  }
  const system = pageProduct(page, systems);
  if (!system || !productPages[system.sku]) throw new Error(`Missing public product: ${page.sku}`);
  const name = system.name.replace("JON PC ", "");
  return { path: productHref(system.sku), title: `${name} ${system.productRange === "Compact AI" ? "Mini PC" : "Gaming PC"} | JON.PC Melbourne`, description: `${name} from JON.PC in West Melbourne. ${system.recommendedFor ?? "Explore full system specifications"}. View specifications, current price and availability, and order online.`, image: productPages[system.sku].image, label: name };
}

export function pageStructuredData(page: StorefrontPage, systems: PreviewSystem[]) {
  const meta = pageMetadata(page, systems);
  const url = siteOrigin + meta.path;
  const system = pageProduct(page, systems);
  const collection = system ? collections[system.productRange === "Compact AI" ? "mini" : "gaming"] : null;
  const crumbs = [{ "@type": "ListItem", position: 1, name: "Home", item: siteOrigin + "/" },
    ...(collection ? [{ "@type": "ListItem", position: 2, name: collection.label, item: siteOrigin + collection.path }] : []),
    { "@type": "ListItem", position: collection ? 3 : 2, name: meta.label, item: url }];
  return {
    "@context": "https://schema.org",
    "@graph": [
      { "@type": "BreadcrumbList", itemListElement: crumbs },
      ...(system ? [{ "@type": "Product", "@id": url + "#product", name: system.name, sku: system.sku,
        url, image: [new URL(meta.image, siteOrigin).href], description: system.description ?? meta.description,
        brand: { "@type": "Brand", name: "JON.PC" },
        additionalProperty: Object.entries(system.specifications).map(([name, value]) => ({ "@type": "PropertyValue", name, value })),
        offers: { "@type": "Offer", url, priceCurrency: "AUD", price: (system.priceCents / 100).toFixed(2),
          availability: `https://schema.org/${system.available && system.saleEnabled ? "InStock" : "OutOfStock"}`,
          itemCondition: "https://schema.org/NewCondition", seller: { "@id": siteOrigin + "/#business" } },
      }] : [{ "@type": page.kind === "gaming" || page.kind === "mini" ? "CollectionPage" : "WebPage", "@id": url,
        name: meta.label, url, description: meta.description,
        ...(page.kind === "gaming" || page.kind === "mini" ? { mainEntity: { "@type": "ItemList", itemListElement: systems
          .filter(system => (system.productRange === "Compact AI") === (page.kind === "mini"))
          .map((system, index) => ({ "@type": "ListItem", position: index + 1, url: siteOrigin + productHref(system.sku), name: system.name })) } } : {}),
      }]),
    ],
  };
}
