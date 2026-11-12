import assert from 'node:assert/strict';
import { readFile, writeFile, mkdir, rm } from 'node:fs/promises';
import { resolve, join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { renderStorefrontDocument } from './storefront-document.mjs';

const dist = resolve('dist');
const origin = 'https://jonpc.com.au';
const api = (process.env.VITE_API_BASE_URL || 'https://jonpc-backend.prouddesert-f3cf5e65.australiaeast.azurecontainerapps.io').replace(/\/$/, '');
const response = await fetch(`${api}/api/systems/preview`, { signal: AbortSignal.timeout(20_000) });
assert(response.ok, `Public catalogue unavailable (${response.status}); leaving the published site unchanged`);
const systems = await response.json();
assert(Array.isArray(systems) && systems.length > 0, 'Cannot publish an empty catalogue');
assert.equal(new Set(systems.map(system => system.sku)).size, systems.length, 'Duplicate catalogue SKU');
for (const system of systems) {
  assert(system.id && system.sku && system.name && Number.isSafeInteger(system.priceCents) && system.priceCents > 0, 'Invalid public product');
  assert(typeof system.available === 'boolean' && typeof system.saleEnabled === 'boolean', 'Missing product availability');
}
const renderer = await import(pathToFileURL(resolve('.prerender/render.js')).href);
const { publicPages, pageMetadata } = renderer;
const home = await readFile(join(dist, 'index.html'), 'utf8');
const clientTemplate = await readFile(join(dist, 'storefront.html'), 'utf8');
const assetTags = clientTemplate.match(/<script\b[^>]*type="module"[^>]*><\/script>|<link\b[^>]*rel="(?:stylesheet|modulepreload)"[^>]*>/g) ?? [];
assert(assetTags.some(tag => tag.startsWith('<script')), 'Storefront client entry is missing');
const manifest = [];
for (const page of publicPages(systems)) {
  const meta = pageMetadata(page, systems);
  const html = renderStorefrontDocument(home, assetTags, page, systems, renderer);
  const folder = join(dist, meta.path);
  await mkdir(folder, { recursive: true });
  await writeFile(join(folder, 'index.html'), html);
  manifest.push({ page, path: meta.path, title: meta.title });
}
assert.equal(new Set(manifest.map(page => page.path)).size, manifest.length, 'Duplicate public URL');
const paths = ['/', ...manifest.map(page => page.path)];
await writeFile(join(dist, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${paths.map(path => `  <url><loc>${origin}${path}</loc></url>`).join('\n')}\n</urlset>\n`);
await writeFile(join(dist, 'storefront-manifest.json'), JSON.stringify(manifest, null, 2));
await rm(join(dist, 'storefront.html'));
await rm(resolve('.prerender'), { recursive: true });
console.log(`Generated ${systems.length} product pages and ${manifest.length - systems.length} category/service pages from the public catalogue.`);
