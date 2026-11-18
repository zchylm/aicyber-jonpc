import { renderToString } from "react-dom/server";
import type { PreviewSystem } from "../api/systems";
import StorefrontView from "./StorefrontView";
import { collections, pageMetadata, pageStructuredData, type StorefrontPage } from "./pages";
import { productModels, productPages } from "../data/productPages";

export { pageMetadata, pageStructuredData };

export function publicPages(systems: PreviewSystem[]): StorefrontPage[] {
  const models = productModels(systems);
  for (const sku of models.keys()) {
    if (!productPages[sku]) throw new Error(`Add a product image and URL for ${sku} before publishing`);
  }
  return [
    ...Array.from(models.keys(), sku => ({ kind: "product" as const, sku })),
    ...Object.keys(collections).map(kind => ({ kind } as StorefrontPage)),
  ];
}

export function renderPage(page: StorefrontPage, systems: PreviewSystem[]) {
  return renderToString(<StorefrontView page={page} systems={systems} />);
}
