import { useEffect, useRef, useState } from "react";
import type { PreviewSystem } from "../api/systems";
import type { RewardPublicSummary } from "../api/rewards";
import BrandLockup from "../components/BrandLockup";
import CompanyFooter from "../components/CompanyFooter";
import { openPolicy } from "../components/policyEvents";
import { gamingMemoryIntro, productHref, productModelSku, productModels, productName, productPages, productStartingPrice, systemRam } from "../data/productPages";
import { navigationItems } from "../data/site";
import { collections, pageMetadata, pageOptions, pageProduct, type StorefrontPage } from "./pages";

const money = (cents: number) => new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD" }).format(cents / 100);

type Props = { page: StorefrontPage; systems: PreviewSystem[]; verified?: boolean; error?: boolean; founderSummary?: RewardPublicSummary | null; onRetry?: () => void };

export default function StorefrontView({ page, systems, verified = false, error = false, founderSummary, onRetry }: Props) {
  const meta = pageMetadata(page, systems);
  const options = pageOptions(page, systems);
  const [selectedSku, setSelectedSku] = useState<string>();
  const system = options.find(option => option.sku === selectedSku) ?? pageProduct(page, systems);
  const ram = system && systemRam(system.sku);
  const showMemory = Boolean(ram && options.length > 1);
  const isMini = system?.productRange === "Compact AI";
  const collection = system ? collections[isMini ? "mini" : "gaming"] : null;
  const purchaseActions = useRef<HTMLDivElement>(null);
  const [showMobilePurchase, setShowMobilePurchase] = useState(false);
  const canBuy = Boolean(verified && system?.available && system.saleEnabled);
  const founderRate = canBuy && founderSummary?.available && founderSummary.programStatus === "ACTIVE" && founderSummary.remainingPositions > 0
    ? founderSummary.currentRateBasisPoints : null;
  // Match FounderDiscountService's integer-cent, half-up rounding; delivery is excluded.
  const saving = system && founderRate ? Math.floor((system.priceCents * founderRate + 5_000) / 10_000) : 0;
  const productPrice = system ? system.priceCents - saving : 0;
  useEffect(() => {
    const target = purchaseActions.current;
    if (!target) return;
    const update = () => setShowMobilePurchase(target.getBoundingClientRect().bottom < 0);
    const frame = window.requestAnimationFrame(update);
    window.addEventListener("scroll", update, { passive: true });
    window.addEventListener("resize", update);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener("scroll", update);
      window.removeEventListener("resize", update);
    };
  }, [productPrice]);
  return <div className="site-shell storefront-shell" id="top">
    <div className="hero-grid" aria-hidden="true" />
    <header className="site-nav">
      <a className="brand" href="/" aria-label="JON. PC home"><BrandLockup /></a>
      <nav className="nav-links" aria-label="Primary navigation">
        {navigationItems.map(item => <a key={item.label} href={`/${item.href}`}>{item.label}</a>)}
      </nav>
      <div className="nav-actions" id="storefront-account" />
    </header>
    <main className="storefront-main">
      <nav className="storefront-breadcrumbs" aria-label="Breadcrumb"><a href="/">Home</a><span aria-hidden="true">/</span>{collection && <><a href={collection.path}>{collection.label}</a><span aria-hidden="true">/</span></>}<span aria-current="page">{meta.label}</span></nav>
      {system ? <>
        <section className={`storefront-product${ram ? " storefront-gaming-product" : ""}`} aria-labelledby="product-title">
          <div className={`storefront-product-image ${isMini ? "storefront-product-image-mini" : ""}`}><img src={productPages[productModelSku(system.sku)].image} alt={meta.label} width="1000" height="750" fetchPriority="high" /></div>
          <div className="storefront-product-copy">
            <h1 id="product-title">{meta.label}<span>.</span></h1>
            <p className="storefront-product-purpose">{system.recommendedFor}</p>
            {showMemory && <fieldset className="storefront-memory">
              <legend>System RAM</legend>
              <div className="storefront-memory-options">{[...options].sort((a, b) => (systemRam(a.sku) ?? 0) - (systemRam(b.sku) ?? 0)).map(option =>
                <label key={option.sku}>
                  <input type="radio" name="system-memory" checked={option.sku === system.sku} onChange={() => setSelectedSku(option.sku)} />
                  <span className="storefront-memory-option">
                    <strong>{systemRam(option.sku)}GB DDR5</strong>
                    <span>{money(option.priceCents)}</span>
                    {(!option.available || !option.saleEnabled) && <small>Unavailable</small>}
                  </span>
                </label>
              )}</div>
            </fieldset>}
            <div className="storefront-price-block" aria-live="polite">
              {saving > 0 && <div className="storefront-founder-summary">
                <span className="storefront-regular-price">Regular price <s>{money(system.priceCents)}</s></span>
                <strong className="storefront-founder-savings">Save {money(saving)} <span>· {founderRate! / 100}% off</span></strong>
              </div>}
              <div className="storefront-price"><strong>{money(productPrice)}</strong><span>{saving > 0 ? "Founder price · " : ""}AUD · incl. GST</span></div>
              {saving > 0 && <p className="storefront-offer-note">
                <button type="button" onClick={() => openPolicy("cashback")}>Limited places at checkout <span aria-hidden="true">↗</span></button>
              </p>}
              {ram && system.specifications.Accessories?.includes("Logitech MK120") && <p className="storefront-included">Includes Logitech MK120 wired keyboard and mouse</p>}
            </div>
            <p className="storefront-availability" role="status">{error ? <>Availability check unavailable. <button type="button" onClick={onRetry}>Try again</button></> : !verified ? "Checking current availability…" : canBuy ? "In stock" : "Currently unavailable"}</p>
            <div className="storefront-product-actions" ref={purchaseActions}>
              <button className="button button-primary" type="button" disabled={!canBuy} onClick={() => window.dispatchEvent(new CustomEvent("jonpc:buy-now", { detail: system }))}>Buy now <span aria-hidden="true">↗</span></button>
              <button className="button storefront-secondary" type="button" disabled={!canBuy} onClick={() => window.dispatchEvent(new CustomEvent("jonpc:add-to-bag", { detail: system }))}>Add to cart <span aria-hidden="true">→</span></button>
            </div>
            <button className="storefront-warranty" type="button" onClick={() => openPolicy("warranty")}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z" /><path d="m8 12 3 3 5-6" /></svg>
              <span className="storefront-warranty-copy">
                <strong>3-year warranty</strong>
                <span>Parts, labour &amp; return shipping</span>
              </span>
              <span className="storefront-warranty-link">View coverage <span aria-hidden="true">↗</span></span>
            </button>
          </div>
        </section>
        <section className="storefront-specifications" aria-labelledby="specifications-title">
          <div><h2 id="specifications-title">Specifications</h2></div>
          <dl>
            {Object.entries(system.specifications).map(([key, value]) => <div key={key}><dt>{key === "Memory" && ram ? "System RAM" : key}</dt><dd>{value}</dd></div>)}
            <div><dt>SKU</dt><dd>{system.sku}</dd></div>
          </dl>
        </section>
        <section className="storefront-next"><div><span className="section-kicker">Find your fit</span><h2>Compare your options.</h2></div><a className="button storefront-secondary" href={collection!.path}>Explore {collection!.label} <span aria-hidden="true">↗</span></a></section>
      </> : page.kind === "gaming" || page.kind === "mini" ? <>
        <section className="storefront-collection-heading"><span className="section-kicker">{collections[page.kind].eyebrow}</span><h1>{page.kind === "gaming" ? <>Gaming PCs.<br /><em>Find your level.</em></> : <>Mini PCs.<br /><em>More possibilities.</em></>}</h1><p>{page.kind === "gaming"
          ? systems.some(item => systemRam(item.sku) === 32) ? gamingMemoryIntro : "Compare graphics, specifications and prices across our gaming PCs."
          : "Compact systems for everyday work and local AI. Compare compute, memory and storage to find the right Edge for your desk."}</p></section>
        <section className="storefront-collection-grid" aria-label={meta.label}>{[...productModels(systems)]
          .filter(([, variants]) => (variants[0].productRange === "Compact AI") === (page.kind === "mini"))
          .map(([sku, variants]) => {
            const starting = productStartingPrice(variants);
            const name = productName(variants[0].name);
            const href = productHref(sku);
            return <article className="storefront-card" key={sku}>
              <a className="storefront-card-image" href={href}><img src={productPages[sku]?.image} alt={name} width="1000" height="750" /></a>
              <div className="storefront-card-copy">
                <span>{starting.available ? "In stock" : "Currently unavailable"}</span>
                <h2><a href={href}>{name}</a></h2><p>{variants[0].recommendedFor}</p>
                <strong>{starting.from && "From "}{money(starting.priceCents)} <small>AUD · incl. GST</small></strong>
                <a className="storefront-card-link" href={href}>{page.kind === "gaming" ? "Choose configuration" : "View details"} <span aria-hidden="true">↗</span></a>
              </div>
            </article>;
          })}</section>
        <section className="storefront-next"><div><span className="section-kicker">Built around you</span><h2>{page.kind === "gaming" ? "Want to choose your own parts?" : "Need a larger AI platform?"}</h2></div><a className="button storefront-secondary" href={page.kind === "gaming" ? collections.custom.path : collections.ai.path}>{page.kind === "gaming" ? "Explore Custom PCs" : "Explore AI Solutions"} <span aria-hidden="true">↗</span></a></section>
      </> : page.kind === "custom" || page.kind === "ai" ? <ServicePage kind={page.kind} /> : null}
    </main>
    <CompanyFooter />
    {system && showMobilePurchase && <aside className="storefront-mobile-purchase" aria-label="Quick purchase"><div><span>{ram ? `${ram}GB RAM · ` : ""}{saving > 0 ? "Founder price · incl. GST" : "incl. GST"}</span><strong>{money(productPrice)}</strong></div><button className="button button-primary" type="button" disabled={!canBuy} onClick={() => window.dispatchEvent(new CustomEvent("jonpc:buy-now", { detail: system }))}>Buy now <span aria-hidden="true">↗</span></button></aside>}
  </div>;
}

function ServicePage({ kind }: { kind: "custom" | "ai" }) {
  const custom = kind === "custom";
  return <>
    <section className={`storefront-service-hero storefront-service-${kind}`}><div><span className="section-kicker">{collections[kind].eyebrow}</span><h1>{custom ? <>Custom PCs.<br /><em>Built for you.</em></> : <>AI solutions.<br /><em>Built to scale.</em></>}</h1><p>{custom ? "Choose the parts, look and performance that fit your life. We review your configuration and availability before sending your final quote." : "From a focused local-AI workstation to a multi-node deployment. Tell us what your work needs, and we will review the hardware and integration scope with you."}</p><a className="button button-primary" href={custom ? "/#build" : "/#ai-infrastructure"}>{custom ? "Configure your PC" : "Plan your AI system"} <span aria-hidden="true">↗</span></a><span className="storefront-quote-note">Quote after review</span></div></section>
    <section className="storefront-process" aria-label="How it works">{(custom ? [
      ["Choose your direction", "Start with your workload and budget, then choose compatible components and your preferred case style."],
      ["Reviewed by our team", "We check the configuration, component availability and final price before you commit."],
      ["Approve, then order", "Review your quote in My Orders. Accept it when you are ready to continue to payment."],
    ] : [
      ["Define your workload", "Choose the scale and platform, from tower compute to rack and multi-GPU infrastructure."],
      ["Scope the project", "Specify compute, storage, networking and the integration or software support you need."],
      ["Engineering review", "Our team checks compatibility and availability, then provides a tailored quote for your approval."],
    ]).map(([title, body], index) => <article key={title}><span>0{index + 1}</span><h2>{title}</h2><p>{body}</p></article>)}</section>
    <section className="storefront-next"><div><span className="section-kicker">Prefer a complete system?</span><h2>{custom ? "Explore our gaming desktops." : "Start with compact local AI."}</h2></div><a className="button storefront-secondary" href={custom ? collections.gaming.path : collections.mini.path}>{custom ? "Shop Gaming PCs" : "Shop Mini PCs"} <span aria-hidden="true">↗</span></a></section>
  </>;
}
