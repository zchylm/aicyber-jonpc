import { renderToString } from "react-dom/server";
import type { PreviewSystem } from "../api/systems";
import StorefrontView from "./StorefrontView";
import { collections, pageMetadata, pageStructuredData, type StorefrontPage } from "./pages";
import { productPages } from "../data/productPages";

export { pageMetadata, pageStructuredData };

export function publicPages(systems: PreviewSystem[]): StorefrontPage[] {
  for (const system of systems) {
    if (!productPages[system.sku]) throw new Error(`Add a product image and URL for ${system.sku} before publishing`);
  }
  return [
    ...systems.map(system => ({ kind: "product" as const, sku: system.sku })),
    ...Object.keys(collections).map(kind => ({ kind } as StorefrontPage)),
  ];
}

export function renderPage(page: StorefrontPage, systems: PreviewSystem[]) {
  return renderToString(<StorefrontView page={page} systems={systems} />);
}
