import { useEffect, useRef, useState } from "react";
import type { PreviewSystem } from "../api/systems";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import strikeImage from "../assets/featured-systems/strike-5060.jpeg";
import strikeTiImage from "../assets/featured-systems/strike-ti.jpeg";
import stormImage from "../assets/featured-systems/storm-5070.jpeg";
import stormTiImage from "../assets/featured-systems/storm-ti.jpeg";
import MiniSystemsCatalog from "./MiniSystemsCatalog";
import "./FirstBatchCatalog.css";

const sharedSpecOrder = ["CPU", "Memory", "Storage", "Motherboard", "Cooler", "PSU", "Case", "Fans", "Networking", "Accessories", "OS"];

function money(cents: number) { return `$${(cents / 100).toLocaleString("en-AU")} AUD`; }
const systemImages = [strikeImage, strikeTiImage, stormImage, stormTiImage];

function FirstBatchCatalog({ systems }: { systems: PreviewSystem[] }) {
  const [selected, setSelected] = useState<PreviewSystem | null>(null);
  const [detailHistoryOpen, setDetailHistoryOpen] = useState(false);
  const [compareOpen, setCompareOpen] = useState(false);
  const [allSpecs, setAllSpecs] = useState(false);
  const lastSelected = useRef<PreviewSystem | null>(null);
  const { open: openDetailPanel, close: closeDetailPanel, dismiss: dismissDetailPanel } = useHistoryPanel("system-detail", detailHistoryOpen, setDetailHistoryOpen);
  const { open: openComparePanel, close: closeComparePanel } = useHistoryPanel("system-compare", compareOpen, setCompareOpen);
  const desktopSystems = systems.filter((system) => system.productRange !== "Compact AI");
  const miniSystems = systems.filter((system) => system.productRange === "Compact AI");
  const sorted = [...desktopSystems].sort((a, b) => a.priceCents - b.priceCents);
  const shared = sorted.length
    ? sharedSpecOrder.filter((key) => sorted.every((system) => system.specifications[key] === sorted[0].specifications[key]))
    : [];

  useEffect(() => {
    if (detailHistoryOpen && !selected && lastSelected.current) setSelected(lastSelected.current);
    if (!detailHistoryOpen && selected) setSelected(null);
  }, [detailHistoryOpen, selected]);

  function showSystem(system: PreviewSystem) {
    lastSelected.current = system;
    setSelected(system);
    openDetailPanel();
  }

  function addToBag(system: PreviewSystem) {
    if (!system.saleEnabled || !system.available) return;
    window.dispatchEvent(new CustomEvent("jonpc:add-to-bag", { detail: system }));
  }

  return <section className="systems-section first-batch-section" id="systems" aria-labelledby="systems-title">
    <div className="systems-heading"><div><span className="section-kicker">JON.PC / Pre-configured systems</span><h2 id="systems-title">Complete systems.<br /><em>Ready to choose.</em></h2></div><p>Desktop PCs for play and work. Compact systems for local AI.</p></div>
    <div className="first-batch-toolbar"><div><span>JON.PC desktop systems</span><strong>Strike → Storm</strong></div><button type="button" onClick={() => { openComparePanel(); setAllSpecs(false); }} aria-haspopup="dialog">Compare desktops <span aria-hidden="true">↗</span></button></div>
    <div className="first-batch-grid">{sorted.map((system, index) => <article className="first-batch-card" key={system.id}>
      <div className={`first-batch-visual first-batch-visual-${index + 1}`}><img src={systemImages[index % systemImages.length]} alt={`${system.name} illustrative PC preview`} /><span className="first-batch-visual-label">JON. PC / 0{index + 1} · Illustrative</span></div>
      <div className="first-batch-card-body"><div className="first-batch-card-top"><span>{system.available ? "In stock" : "Unavailable"}</span></div><h3>{system.name.replace("JON PC ", "")}</h3><p>{system.recommendedFor}</p><div className="first-batch-card-bottom"><strong>{money(system.priceCents)} <small>incl. GST</small></strong><div className="first-batch-card-actions"><button type="button" onClick={() => showSystem(system)}>View system <span aria-hidden="true">↗</span></button><button type="button" className="first-batch-request" onClick={() => addToBag(system)} disabled={!system.saleEnabled || !system.available}>Add to cart <span aria-hidden="true">→</span></button></div></div></div>
    </article>)}</div>
    <MiniSystemsCatalog systems={miniSystems} onAddToBag={addToBag} />
    {selected && <div className="first-batch-overlay" onMouseDown={(event) => event.target === event.currentTarget && closeDetailPanel()}><section className="first-batch-dialog" role="dialog" aria-modal="true" aria-labelledby="first-batch-detail-title"><button className="first-batch-close" type="button" onClick={closeDetailPanel} aria-label="Close">×</button><span className="section-kicker">JON. PC / System detail</span><h3 id="first-batch-detail-title">{selected.name.replace("JON PC ", "")}</h3><p>{selected.description}</p><div className="first-batch-detail-highlights"><div><span>Graphics</span><strong>{selected.specifications.GPU}</strong></div><div><span>Designed for</span><strong>{selected.recommendedFor}</strong></div><div><span>Price incl. GST</span><strong>{money(selected.priceCents)}</strong></div></div><div className="first-batch-detail-dispatch"><span>{selected.available ? "In stock" : "Availability"}</span><strong>{selected.available && selected.dispatchEstimate ? `Estimated dispatch: ${selected.dispatchEstimate}` : "Currently unavailable"}</strong></div><details className="first-batch-full-specs"><summary>Full specifications</summary>{Object.entries(selected.specifications).map(([key, value]) => <div key={key}><span>{key}</span><strong>{value}</strong></div>)}</details><button className="first-batch-advice" type="button" onClick={() => addToBag(systems.find((item) => item.id === selected.id) ?? selected)} disabled={!selected.saleEnabled || !selected.available}>Add to cart <span aria-hidden="true">↗</span></button><button className="first-batch-advice first-batch-advice-secondary" type="button" onClick={() => { dismissDetailPanel(); window.dispatchEvent(new CustomEvent("jonpc:open-ai")); }}>Questions? Ask JON.AI <span aria-hidden="true">↗</span></button></section></div>}
    {compareOpen && <div className="first-batch-overlay" onMouseDown={(event) => event.target === event.currentTarget && closeComparePanel()}><section className="first-batch-dialog first-batch-compare" role="dialog" aria-modal="true" aria-labelledby="first-batch-compare-title"><button className="first-batch-close" type="button" onClick={closeComparePanel} aria-label="Close">×</button><span className="section-kicker">JON. PC / Compare</span><h3 id="first-batch-compare-title">Choose your graphics.</h3><p>Same core platform. Find the level that fits your play.</p><span className="first-batch-compare-hint">Swipe to compare <span aria-hidden="true">→</span></span><div className="first-batch-compare-scroll"><table><thead><tr><th>Difference</th>{sorted.map((system) => <th key={system.id}>{system.name.replace("JON PC ", "")}</th>)}</tr></thead><tbody><tr><th>Graphics</th>{sorted.map((system) => <td key={system.id}>{system.specifications.GPU}</td>)}</tr><tr><th>Designed for</th>{sorted.map((system) => <td key={system.id}>{system.recommendedFor}</td>)}</tr><tr><th>Price incl. GST</th>{sorted.map((system) => <td key={system.id}>{money(system.priceCents)}</td>)}</tr></tbody></table></div><button className="first-batch-expand" type="button" onClick={() => setAllSpecs(!allSpecs)} aria-expanded={allSpecs}>{allSpecs ? "Hide shared specifications" : "Show shared specifications"} <span aria-hidden="true">{allSpecs ? "−" : "+"}</span></button>{allSpecs && <div className="first-batch-shared">{shared.map((key) => <div key={key}><span>{key}</span><strong>{sorted[0].specifications[key]}</strong></div>)}</div>}</section></div>}
  </section>;
}

export default FirstBatchCatalog;
