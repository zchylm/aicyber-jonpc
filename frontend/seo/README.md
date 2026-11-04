# JON.PC search foundation

The homepage metadata and JSON-LD are in `index.html`, so crawlers can read them
without running JavaScript. Page content still renders through React. Independent,
pre-rendered category pages are a later phase; homepage anchors are not separate
indexable pages and must not be listed in the sitemap.

## Published assets

- Canonical homepage: https://jonpc.com.au/
- Sitemap: https://jonpc.com.au/sitemap.xml
- Crawl rules: https://jonpc.com.au/robots.txt
- Sharing image: https://jonpc.com.au/social/jonpc-share.jpg (1200 × 630)
- Official logo: https://jonpc.com.au/brand/jonpc-logo.png
- Favicon: https://jonpc.com.au/favicon.png (96 × 96)
- Apple touch icon: https://jonpc.com.au/apple-touch-icon.png (180 × 180)

`brand-assets.html` is the editable HTML/CSS source for the exported images. It
uses the existing official logo, not a generated replacement. Open it through
the Vite development server. Export `.share-card` as a 1200 × 630 JPEG and
`.favicon` as a 96 × 96 PNG; use a device scale factor of 1.875 for the 180 × 180
touch icon. These source files are not included in the production build.

Run `npm run build && npm run check:seo` after changing metadata or public assets.
The deployment workflow runs the same checks before publishing.

## Company account steps

1. Sign in to https://search.google.com/search-console/ with a company-owned Google
   account. If the property already exists, ask its verified owner for access.
2. Prefer a Domain property for `jonpc.com.au`. Add only the exact verification
   TXT record Google supplies through the domain's DNS provider, then verify.
   Do not replace existing DNS records. A URL-prefix property for
   `https://jonpc.com.au/` with Google's supplied HTML tag or file is an alternative.
3. Submit `https://jonpc.com.au/sitemap.xml` under Sitemaps.
4. Inspect `https://jonpc.com.au/` using URL Inspection and its live test, then
   request indexing. Review Page indexing and Performance over time.
5. Check an existing Google Business Profile (or create one if needed). Keep the
   name, address, phone and appointment details consistent with the website.

No verification token is included until Google provides one for the company's
account. Public source and DNS checks cannot establish whether another owner
already has Search Console access.

## Content accuracy

The business entity is `ComputerStore`, a `LocalBusiness` subtype, with the
confirmed legal name, support email and West Melbourne address. The telephone
number is withheld during the initial launch in both the footer and JSON-LD. No opening
hours, ratings, reviews, social accounts or product price/stock data are invented.
Keep this entity consistent with visible website details. Product schema needs
its own page/data strategy before being added.

Structured data eligibility and local checks do not guarantee Google's display,
ranking, or AI Overview wording. Use Google's Rich Results Test and Search
Console for external validation after publication.

References:
- https://developers.google.com/search/docs/appearance/structured-data/local-business
- https://developers.google.com/search/docs/appearance/favicon-in-search
- https://support.google.com/webmasters/answer/9008080
- https://developers.google.com/search/docs/crawling-indexing/sitemaps/overview
