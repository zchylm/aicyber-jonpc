# Public product and category pages

The homepage remains the existing application. Public landing pages are separate generated HTML documents, compatible with GitHub Pages directory URLs. Homepage navigation and Hero category controls retain their original in-page anchors. View system retains the original gaming/mini detail dialogs, including specifications, purchase actions and Back/close behavior. Product names and an explicit Open product page link provide crawlable/shareable product URLs. No history-router fallback or hosting migration is required.

An ordinary homepage reload clears the previous module fragment and starts at Hero. Fresh incoming module links still scroll to their target. Explicit orders, payment, account-verification/reset, saved-build and savings query links retain their action state on reload. This does not clear account/cart storage or alter independent product-page reloads.

## Content and build

- `frontend/src/data/productPages.ts` contains only SKU → URL/image presentation mappings. It contains no prices.
- `frontend/src/storefront/` renders product, category and service content on the server during the build and hydrates that same content in the browser.
- The build reads the existing public `/api/systems/preview` catalogue. It refuses an empty/unavailable/invalid catalogue or a product without an image/URL mapping. The deployment workflow then stops before uploading an incomplete artifact.
- Each product document contains its name, image, specifications, base product price and availability in the original HTML, plus its own title, description, canonical, sharing metadata, Product/Offer and breadcrumb data.
- The generated sitemap includes the homepage, eight current products, and Gaming PCs, Mini PCs, Custom PCs and AI Solutions landing pages. Custom/AI pages describe the review process and link into the existing configurators; they do not publish estimated prices or create an Offer.
- `npm run build` now performs the regular Vite build, a temporary server render build, and HTML/sitemap generation. The temporary `.prerender` directory is removed after success. `npm run check:seo` validates the generated documents and assets.
- Production uses the existing GitHub Actions environment. No API credentials are needed for public-catalogue generation. A focused browser check now runs before uploading the Pages artifact; it checks original navigation and dialogs as well as the new documents.

## Live data and checkout

HTML is a publication-time catalogue snapshot. The client verifies current prices and availability when opened, on focus, every minute, and after stock/payment events. Visible product prices and Product/Offer JSON-LD update together. Older responses cannot overwrite a newer response. Purchase controls remain disabled until verification succeeds, or when the product is missing/unavailable or verification fails.

Price, specifications, product publication and stock changes should be followed by a frontend rebuild to refresh the original HTML snapshot. Live refresh and backend checkout validation remain necessary between builds. This change does not implement a Merchant Center feed, automated deployments on stock changes, or guaranteed Google indexing.

The new pages reuse the existing AuthPanel, ShoppingBag, PaymentCheckout, PolicyPanel and AI assistant. Session/cart storage stays on the same origin; Stripe returns retain the existing homepage URL. Saved-configuration continuation and savings actions return to the homepage, wait for the existing account refresh, and restore/open the requested view. The server still checks account ownership and computes order totals through existing APIs.

## Local development and review

`npm run dev` serves the original homepage and also renders the new product/category URLs. The development-only middleware handles just those document paths, using the same HTML renderer as the production build and the configured API (default local backend 8081). Unknown product paths return 404 instead of the homepage. No compiled production client is served in development.

To inspect the generated static documents, use a build and preview server. For example, with the local backend on 8081 and port 5174 free:

```sh
cd frontend
VITE_API_BASE_URL=http://127.0.0.1:8081 VITE_JONPC_BANK_TRANSFER=true VITE_JONPC_STRIPE=true VITE_JONPC_STRIPE_TEST_MODE=true npm run build
npm run check:seo
npm run preview -- --host 127.0.0.1 --port 5174 --strictPort
```

Use local test payment/email settings when performing transaction tests. The page-generation step makes public read-only catalogue requests.

For the 2026-11-12 review, a separate generated-page preview is available on 127.0.0.1:5175 with a temporary proxy to the isolated QA backend on 8082. That backend uses `jonpc_seo_pages_20261112`, disables email dispatch and Stripe, and does not use production records. This temporary preview configuration is not part of the production build.

## Verification before publishing

Run build, SEO checks and scoped lint. The focused regression suite is `npm run test:storefront`. With development servers on 127.0.0.1:5174 / localhost:5174 and the generated preview on 127.0.0.1:5175, it covers all three environments. Set JONPC_TEST_BASE_URL to test a single server. Use PLAYWRIGHT_BROWSER_CHANNEL=chrome for an installed Chrome, or install the Playwright Chromium browser first. CI starts its own preview and runs the same checks before deployment.

These are read-only guest checks. Catalogue responses use the build snapshot for determinism, and browser API writes are prohibited. Development document rendering still reads its public backend catalogue. Tests assert actual viewport positioning, all eight original detail dialogs, full specifications, Back/close, comparisons, guest cart, direct product URLs/refresh, and navigation back from new pages. They are not a whole-site regression suite. Inspect direct product/category URLs, refresh, mobile widths, links to the existing configurators, guest and account cart behavior, quote/checkout entry, footer policies, unavailable/error states, and saved-build/savings continuation. Use a plain static server to verify real files and 404 behavior rather than relying only on an SPA preview fallback.

After an approved deployment, check the live documents and sitemap, submit the sitemap to Search Console, and inspect representative product/category URLs and Product markup. Indexing and Merchant Center eligibility need separate Google-side verification.
