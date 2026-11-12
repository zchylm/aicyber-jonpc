import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { resolve, join } from 'node:path';

const dist = resolve('dist');
const html = readFileSync(join(dist, 'index.html'), 'utf8');
const home = 'https://jonpc.com.au/';
const title = html.match(/<title>([^<]+)<\/title>/)?.[1].replaceAll('&amp;', '&');
assert(title?.includes('Gaming PCs') && title.includes('Melbourne') && title.includes('JON.PC'));
assert.equal((html.match(/rel="canonical"/g) ?? []).length, 1);
assert(html.includes(`rel="canonical" href="${home}"`));

function meta(key) {
  const tags = [...html.matchAll(/<meta\s+[^>]+>/g)].map(([tag]) =>
    Object.fromEntries([...tag.matchAll(/([\w:]+)="([^"]*)"/g)].map(([, name, value]) => [name, value.replaceAll('&amp;', '&')])));
  const matches = tags.filter(tag => (tag.name ?? tag.property) === key);
  assert.equal(matches.length, 1, `${key} must occur exactly once`);
  assert(matches[0].content?.trim(), `${key} must not be empty`);
  return matches[0].content;
}

assert(meta('description').includes('West Melbourne'));
assert.equal(meta('og:title'), title);
assert.equal(meta('og:url'), home);
assert.equal(meta('og:type'), 'website');
assert.equal(meta('og:locale'), 'en_AU');
assert.equal(meta('og:site_name'), 'JON.PC');
assert.equal(meta('twitter:card'), 'summary_large_image');
assert.equal(meta('twitter:title'), title);
assert.equal(meta('twitter:image'), meta('og:image'));
for (const key of ['og:description', 'og:image:alt', 'twitter:description', 'twitter:image:alt']) meta(key);
assert.equal(meta('og:image:width'), '1200');
assert.equal(meta('og:image:height'), '630');

function publicAsset(url) {
  const parsed = new URL(url);
  assert.equal(parsed.origin, new URL(home).origin);
  const file = join(dist, parsed.pathname);
  assert(existsSync(file), `Missing public asset: ${parsed.pathname}`);
  return file;
}
const shareImage = readFileSync(publicAsset(meta('og:image')));
assert.equal(shareImage.readUInt16BE(0), 0xffd8, 'Sharing image must be JPEG');
const graphs = [...html.matchAll(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/g)];
assert.equal(graphs.length, 1);
const data = JSON.parse(graphs[0][1]);
assert.equal(data['@context'], 'https://schema.org');
const business = data['@graph'].find(item => item['@type'] === 'ComputerStore');
const website = data['@graph'].find(item => item['@type'] === 'WebSite');
assert.equal(business.name, 'JON.PC');
assert.equal(business.url, home);
assert.equal(business.address.addressLocality, 'West Melbourne');
assert.equal(business.address.postalCode, '3003');
assert.equal(business.email, 'support@jonpc.com.au');
assert.equal(website.publisher['@id'], business['@id']);
publicAsset(business.logo);
assert.equal(business.image, meta('og:image'));

for (const [file, size] of [['favicon.png', 96], ['apple-touch-icon.png', 180]]) {
  assert(html.includes(`href="/${file}"`));
  const png = readFileSync(join(dist, file));
  assert.equal(png.subarray(1, 4).toString(), 'PNG');
  assert.equal(png.readUInt32BE(16), size);
  assert.equal(png.readUInt32BE(20), size);
}
const robots = readFileSync(join(dist, 'robots.txt'), 'utf8');
assert(robots.includes('User-agent: *') && robots.includes('Allow: /'));
assert(!/^Disallow:\s*\/\s*$/m.test(robots), 'Homepage crawling must not be blocked');
assert(robots.includes(`Sitemap: ${home}sitemap.xml`));
const sitemap = readFileSync(join(dist, 'sitemap.xml'), 'utf8');
assert(sitemap.includes('http://www.sitemaps.org/schemas/sitemap/0.9'));
const pages = JSON.parse(readFileSync(join(dist, 'storefront-manifest.json'), 'utf8'));
const urls = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map(match => match[1]);
assert.deepEqual(urls, [home, ...pages.map(page => new URL(page.path, home).href)]);
assert.equal(new Set(urls).size, urls.length, 'Sitemap URLs must be unique');
const titles = new Set([title]);
for (const page of pages) {
  const document = readFileSync(join(dist, page.path, 'index.html'), 'utf8');
  const url = new URL(page.path, home).href;
  assert.equal((document.match(/<h1\b/g) ?? []).length, 1, `${page.path} needs one visible H1`);
  assert(!/<meta[^>]*name="robots"[^>]*noindex/i.test(document));
  assert.equal((document.match(/rel="canonical"/g) ?? []).length, 1);
  assert(document.includes(`rel="canonical" href="${url}"`));
  assert(document.includes(`property="og:url" content="${url}"`));
  const pageTitle = document.match(/<title>([^<]+)<\/title>/)[1].replaceAll('&amp;', '&');
  assert(!titles.has(pageTitle), `Duplicate title: ${pageTitle}`); titles.add(pageTitle);
  const graph = JSON.parse(document.match(/<script id="storefront-schema"[^>]*>([\s\S]*?)<\/script>/)[1])['@graph'];
  assert(graph.some(item => item['@type'] === 'BreadcrumbList'));
  const snapshot = JSON.parse(document.match(/<script id="storefront-data"[^>]*>([\s\S]*?)<\/script>/)[1]);
  const visible = document.split('<script id="storefront-data"')[0];
  for (const [, src] of visible.matchAll(/<img[^>]+src="([^"]+)"/g)) publicAsset(new URL(src, home).href);
  for (const [, asset] of document.matchAll(/(?:src|href)="([^"]*\/assets\/[^"]+)"/g)) publicAsset(new URL(asset, home).href);
  if (page.page.kind === 'product') {
    const product = graph.find(item => item['@type'] === 'Product');
    const system = snapshot.systems.find(system => system.sku === page.page.sku);
    assert.equal(product.sku, system.sku);
    assert.equal(product.offers.url, url);
    assert.equal(product.offers.priceCurrency, 'AUD');
    assert.equal(product.offers.price, (system.priceCents / 100).toFixed(2));
    assert.equal(product.offers.availability, `https://schema.org/${system.available && system.saleEnabled ? 'InStock' : 'OutOfStock'}`);
    assert.equal(product.additionalProperty.length, Object.keys(system.specifications).length);
    assert(visible.includes(system.sku), 'Product SKU must be present without JavaScript');
    assert(visible.includes(new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' }).format(system.priceCents / 100)), 'Price must be present without JavaScript');
    publicAsset(product.image[0]);
  }
}
console.log(`SEO checks passed: homepage and ${pages.length} independent pages, visible HTML, unique metadata, product data, local assets and sitemap.`);
