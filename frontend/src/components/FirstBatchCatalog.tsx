import { useState } from "react";
import type { PreviewSystem } from "../api/systems";
import { defaultProductOption, gamingMemoryIntro, productHref, productModels, productName, productPages, productStartingPrice, systemRam } from "../data/productPages";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import MiniSystemsCatalog from "./MiniSystemsCatalog";
import PcAdviceLink from "./PcAdviceLink";
import "./FirstBatchCatalog.css";

const sharedSpecOrder = ["CPU", "Storage", "Motherboard", "Cooler", "PSU", "Case", "Fans", "Networking", "OS"];

function money(cents: number) { return `A$${(cents / 100).toLocaleString("en-AU")}`; }

function FirstBatchCatalog({ systems }: { systems: PreviewSystem[] }) {
  const [compareOpen, setCompareOpen] = useState(false);
  const [allSpecs, setAllSpecs] = useState(false);
  const { open: openComparePanel, close: closeComparePanel } = useHistoryPanel("system-compare", compareOpen, setCompareOpen);
  const gamingSystems = systems.filter(system => systemRam(system.sku));
  const miniSystems = systems.filter(system => system.productRange === "Compact AI");
  const models = [...productModels(gamingSystems)].map(([sku, options]) => ({
    sku, options, defaultOption: defaultProductOption(sku, options), starting: productStartingPrice(options),
  })).sort((a, b) => a.starting.priceCents - b.starting.priceCents);
  const hasRamOptions = models.length > 0 && models.every(model => model.options.length > 1);
  const shared = gamingSystems.length
    ? sharedSpecOrder.filter(key => gamingSystems[0].specifications[key]
      && gamingSystems.every(system => system.specifications[key] === gamingSystems[0].specifications[key]))
    : [];

  return <section className="systems-section first-batch-section" id="systems" aria-labelledby="systems-title">
    <div className="systems-heading"><div><span className="section-kicker">JON.PC / Shop our PCs</span><h2 id="systems-title">Find your<br /><em>next PC.</em></h2></div><p>Gaming desktops and compact AI PCs.<br />Choose the system that fits your world.</p></div>
    <div className="first-batch-toolbar catalog-category-heading" id="gaming-pcs">
      <div><span>01 / Strike &amp; Storm series</span><h3>Gaming PCs<span aria-hidden="true">.</span></h3><p>{hasRamOptions ? gamingMemoryIntro : "Built for play. Choose your level."}</p></div>
      <div className="first-batch-tools">
        <PcAdviceLink />
        <button type="button" onClick={() => { openComparePanel(); setAllSpecs(false); }} aria-haspopup="dialog">Compare gaming PCs <span aria-hidden="true">↗</span></button>
      </div>
    </div>
    <div className="first-batch-grid">{models.map(({ sku, starting, defaultOption }) => {
      const name = productName(defaultOption.name);
      const href = productHref(sku);
      return <article className="first-batch-card" key={sku}>
        <a className="first-batch-visual first-batch-gaming-visual" href={href} aria-label={`View ${name} product page`}><img src={productPages[sku].image} alt={`${name} illustrative PC preview`} loading="lazy" width={1000} height={750} /></a>
        <div className="first-batch-card-body">
          <div className="first-batch-card-top"><span>{starting.available ? "In stock" : "Unavailable"}</span></div>
          <h3><a href={href}>{name}</a></h3>
          <p>{defaultOption.recommendedFor}</p>
          <div className="first-batch-card-bottom">
            <strong>{starting.from && <span className="first-batch-from">From </span>}{money(starting.priceCents)} <small>incl. GST</small></strong>
            <a className="first-batch-configure" href={href}>Choose configuration <span aria-hidden="true">↗</span></a>
          </div>
        </div>
      </article>;
    })}</div>
    <MiniSystemsCatalog systems={miniSystems} />
    {compareOpen && <div className="first-batch-overlay" onMouseDown={event => event.target === event.currentTarget && closeComparePanel()}>
      <section className="first-batch-dialog first-batch-compare" role="dialog" aria-modal="true" aria-labelledby="first-batch-compare-title">
        <button className="first-batch-close" type="button" onClick={closeComparePanel} aria-label="Close">×</button>
        <span className="section-kicker">JON. PC / Compare</span>
        <h3 id="first-batch-compare-title">Compare gaming PCs.</h3>
        <p>Compare graphics and memory. Prices include GST.</p>
        <span className="first-batch-compare-hint">Swipe to compare <span aria-hidden="true">→</span></span>
        <div className="first-batch-compare-scroll">
          <table>
            <thead><tr><th scope="col">Specification</th>{models.map(({ sku, defaultOption }) => <th scope="col" key={sku}>{productName(defaultOption.name)}</th>)}</tr></thead>
            <tbody>
              <tr><th scope="row">Graphics</th>{models.map(({ sku, defaultOption }) => <td key={sku}>{defaultOption.specifications.GPU}</td>)}</tr>
              <tr><th scope="row">Designed for</th>{models.map(({ sku, defaultOption }) => <td key={sku}>{defaultOption.recommendedFor}</td>)}</tr>
              {[16, 32].map(capacity => <tr key={capacity}><th scope="row">{capacity}GB RAM</th>{models.map(({ sku, options, defaultOption }) => {
                const option = options.find(item => systemRam(item.sku) === capacity);
                return <td key={sku}>{option ? <>
                  {money(option.priceCents)}
                  {option === defaultOption && <small>Default</small>}
                  {(!option.available || !option.saleEnabled) && <small>Unavailable</small>}
                </> : "—"}</td>;
              })}</tr>)}
              <tr><th scope="row">Configure</th>{models.map(({ sku, defaultOption }) =>
                <td key={sku}><a aria-label={`Configure ${productName(defaultOption.name)}`} href={productHref(sku)}>Configure <span aria-hidden="true">↗</span></a></td>
              )}</tr>
            </tbody>
          </table>
        </div>
        {shared.length > 0 && <button className="first-batch-expand" type="button" onClick={() => setAllSpecs(!allSpecs)} aria-expanded={allSpecs}>{allSpecs ? "Hide shared specifications" : "Show shared specifications"} <span aria-hidden="true">{allSpecs ? "−" : "+"}</span></button>}
        {allSpecs && <div className="first-batch-shared">{shared.map(key => <div key={key}><span>{key}</span><strong>{gamingSystems[0].specifications[key]}</strong></div>)}</div>}
      </section>
    </div>}
  </section>;
}

export default FirstBatchCatalog;
