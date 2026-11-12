import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { renderStorefrontDocument } from './storefront-document.mjs';

// Only the new public document paths are handled here. Existing homepage/hash flows stay in Vite.
export function storefrontDevPages() {
  return {
    name: 'jonpc-storefront-dev-pages',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use(async (request, response, next) => {
        const path = new URL(request.url, 'http://localhost').pathname;
        if (!/^\/(?:products\/[^/]+|gaming-pcs|mini-pcs|custom-pcs|ai-solutions)\/?$/.test(path)) return next();
        try {
          const api = server.config.env.VITE_API_BASE_URL || 'http://localhost:8081';
          const catalogue = await fetch(`${api.replace(/\/$/, '')}/api/systems/preview`, { signal: AbortSignal.timeout(10_000) });
          if (!catalogue.ok) throw new Error(`Unable to load public catalogue (${catalogue.status})`);
          const systems = await catalogue.json();
          const renderer = await server.ssrLoadModule('/src/storefront/render.tsx');
          const page = renderer.publicPages(systems).find(page => renderer.pageMetadata(page, systems).path === path.replace(/\/?$/, '/'));
          if (!page) {
            response.statusCode = 404;
            response.end('Product page not found.');
            return;
          }
          const home = await readFile(resolve(server.config.root, 'index.html'), 'utf8');
          const document = renderStorefrontDocument(home, ['<script type="module" src="/src/storefront/client.tsx"></script>'], page, systems, renderer);
          response.setHeader('Content-Type', 'text/html; charset=utf-8');
          response.end(await server.transformIndexHtml(path, document));
        } catch (error) { next(error); }
      });
    },
  };
}
