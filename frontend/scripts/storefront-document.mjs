export function renderStorefrontDocument(home, assetTags, page, systems, { pageMetadata, pageStructuredData, renderPage }) {
  const origin = 'https://jonpc.com.au';
  const meta = pageMetadata(page, systems);
  const canonical = origin + meta.path;
  const baseHead = home.match(/<head>([\s\S]*?)<\/head>/)[1]
    .replace(/<script\b[^>]*type="module"[^>]*><\/script>|<link\b[^>]*rel="(?:stylesheet|modulepreload)"[^>]*>/g, '')
    .replace(/<meta property="og:image:(?:width|height)"[^>]*>/g, '');
  const escape = value => String(value).replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
  const safeJson = value => JSON.stringify(value).replaceAll('<', '\\u003c');
  let head = baseHead.replace(/<title>[\s\S]*?<\/title>/, `<title>${escape(meta.title)}</title>`)
    .replace(/<link rel="canonical"[^>]*>/, `<link rel="canonical" href="${canonical}" />`);
  const metadata = {
    description: meta.description, 'og:title': meta.title, 'twitter:title': meta.title,
    'og:description': meta.description, 'twitter:description': meta.description,
    'og:url': canonical, 'og:image': new URL(meta.image, origin).href,
    'twitter:image': new URL(meta.image, origin).href,
    'og:image:alt': meta.label, 'twitter:image:alt': meta.label,
  };
  for (const [key, value] of Object.entries(metadata)) {
    head = head.replace(new RegExp(`<meta (?:name|property)="${key}"[^>]*>`), `<meta ${key.startsWith('og:') ? 'property' : 'name'}="${key}" content="${escape(value)}" />`);
  }
  head += '\n' + assetTags.join('\n') + `\n<script id="storefront-schema" type="application/ld+json">${safeJson(pageStructuredData(page, systems))}</script>\n`;
  return `<!doctype html>\n<html lang="en"><head>${head}</head><body><div id="storefront-root">${renderPage(page, systems)}</div><div id="storefront-runtime"></div><script id="storefront-data" type="application/json">${safeJson({ page, systems })}</script></body></html>`;
}
