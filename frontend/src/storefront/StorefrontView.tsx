import type { PreviewSystem } from "../api/systems";
import BrandLockup from "../components/BrandLockup";
import CompanyFooter from "../components/CompanyFooter";
import { openPolicy } from "../components/policyEvents";
import { productHref, productPages } from "../data/productPages";
import { navigationItems } from "../data/site";
import { collections, pageMetadata, pageProduct, type StorefrontPage } from "./pages";

const money = (cents: number) => new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD" }).format(cents / 100);

type Props = { page: StorefrontPage; systems: PreviewSystem[]; verified?: boolean; error?: boolean; onRetry?: () => void };

export default function StorefrontView({ page, systems, verified = false, error = false, onRetry }: Props) {
  const meta = pageMetadata(page, systems);
  const system = pageProduct(page, systems);
  const isMini = system?.productRange === "Compact AI";
  const collection = system ? collections[isMini ? "mini" : "gaming"] : null;
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
        <section className="storefront-product" aria-labelledby="product-title">
          <div className={`storefront-product-image ${isMini ? "storefront-product-image-mini" : ""}`}><img src={productPages[system.sku].image} alt={system.name} width="1000" height="750" fetchPriority="high" /></div>
          <div className="storefront-product-copy">
            <span className="section-kicker">{collection?.label} / {system.sku}</span>
            <h1 id="product-title">{meta.label}<span>.</span></h1>
            <p className="storefront-product-purpose">{system.recommendedFor}</p>
            <dl className="storefront-key-specs">{[isMini ? "Processor" : "GPU", "Memory", "Storage"].filter(key => system.specifications[key]).map(key => <div key={key}><dt>{key}</dt><dd>{system.specifications[key]}</dd></div>)}</dl>
            <div className="storefront-price"><strong>{money(system.priceCents)}</strong><span>AUD · incl. GST</span></div>
            <p className="storefront-availability">{system.available && system.saleEnabled ? "In stock" : "Currently unavailable"}<span> · {system.dispatchEstimate ? `Dispatch ${system.dispatchEstimate}` : "Contact us for availability"}</span></p>
            <div className="storefront-product-actions">
              <button className="button button-primary" type="button" disabled={!verified || !system.available || !system.saleEnabled} onClick={() => window.dispatchEvent(new CustomEvent("jonpc:buy-now", { detail: system }))}>Buy now <span aria-hidden="true">↗</span></button>
              <button className="button storefront-secondary" type="button" disabled={!verified || !system.available || !system.saleEnabled} onClick={() => window.dispatchEvent(new CustomEvent("jonpc:add-to-bag", { detail: system }))}>Add to cart <span aria-hidden="true">→</span></button>
            </div>
            <div className="storefront-check" role="status">{error ? <>Availability check unavailable. <button type="button" onClick={onRetry}>Try again</button></> : !verified ? "Checking current availability…" : "Melbourne delivery A$25 · West Melbourne pickup available"}</div>
            <button className="storefront-offer" type="button" onClick={() => openPolicy("cashback")}>Explore the monthly Founder 5% offer <span aria-hidden="true">↗</span></button>
          </div>
        </section>
        <section className="storefront-specifications" aria-labelledby="specifications-title"><div><span className="section-kicker">Know your system</span><h2 id="specifications-title">Every detail.<br /><em>Clearly configured.</em></h2><p>{system.description}</p></div><dl>{Object.entries(system.specifications).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value}</dd></div>)}</dl></section>
        <section className="storefront-next"><div><span className="section-kicker">Find your fit</span><h2>Compare your options.</h2></div><a className="button storefront-secondary" href={collection!.path}>Explore {collection!.label} <span aria-hidden="true">↗</span></a></section>
      </> : page.kind === "gaming" || page.kind === "mini" ? <>
        <section className="storefront-collection-heading"><span className="section-kicker">{collections[page.kind].eyebrow}</span><h1>{page.kind === "gaming" ? <>Gaming PCs.<br /><em>Find your level.</em></> : <>Mini PCs.<br /><em>More possibilities.</em></>}</h1><p>{page.kind === "gaming" ? "From your first gaming desktop to your next upgrade. Compare the graphics, see the complete specification and choose your system." : "Compact systems for everyday work and local AI. Compare compute, memory and storage to find the right Edge for your desk."}</p></section>
        <section className="storefront-collection-grid" aria-label={meta.label}>{systems.filter(item => (item.productRange === "Compact AI") === (page.kind === "mini")).map(item => <article className="storefront-card" key={item.id}><a className="storefront-card-image" href={productHref(item.sku)}><img src={productPages[item.sku]?.image} alt={item.name} width="1000" height="750" /></a><div className="storefront-card-copy"><span>{item.available && item.saleEnabled ? "In stock" : "Currently unavailable"}</span><h2><a href={productHref(item.sku)}>{item.name.replace("JON PC ", "")}</a></h2><p>{item.recommendedFor}</p><strong>{money(item.priceCents)} <small>AUD · incl. GST</small></strong><a className="storefront-card-link" href={productHref(item.sku)}>Explore system <span aria-hidden="true">↗</span></a></div></article>)}</section>
        <section className="storefront-next"><div><span className="section-kicker">Built around you</span><h2>{page.kind === "gaming" ? "Want to choose your own parts?" : "Need a larger AI platform?"}</h2></div><a className="button storefront-secondary" href={page.kind === "gaming" ? collections.custom.path : collections.ai.path}>{page.kind === "gaming" ? "Explore Custom PCs" : "Explore AI Solutions"} <span aria-hidden="true">↗</span></a></section>
      </> : page.kind === "custom" || page.kind === "ai" ? <ServicePage kind={page.kind} /> : null}
    </main>
    <CompanyFooter />
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
