import { useState } from "react";
import edgeMiniImage from "../assets/minipc/edge-mini.jpg";
import edgeAiImage from "../assets/minipc/edge-ai.jpg";
import edgeProImage from "../assets/minipc/edge-pro.jpg";
import edgeMaxImage from "../assets/minipc/edge-max.jpg";
import type { PreviewSystem } from "../api/systems";
import { productHref } from "../data/productPages";
import { useHistoryPanel } from "../hooks/useHistoryPanel";

const images: Record<string, string> = {
  "JON-EDGE-MINI": edgeMiniImage,
  "JON-EDGE-AI": edgeAiImage,
  "JON-EDGE-PRO": edgeProImage,
  "JON-EDGE-MAX": edgeMaxImage,
};

function money(cents: number) { return `$${(cents / 100).toLocaleString("en-AU")} AUD`; }

function MiniSystemsCatalog({ systems }: { systems: PreviewSystem[] }) {
  const [compareOpen, setCompareOpen] = useState(false);
  const { open: openComparePanel, close: closeComparePanel } = useHistoryPanel("mini-system-compare", compareOpen, setCompareOpen);

  return <div className="mini-systems-catalog" id="compact-ai-systems">
    <div className="first-batch-toolbar first-batch-toolbar-mini catalog-category-heading"><div><span>02 / Edge series</span><h3>Mini PCs<span aria-hidden="true">.</span></h3><p>Compact PCs for work and local AI.</p></div><button type="button" onClick={openComparePanel} aria-haspopup="dialog">Compare mini PCs <span aria-hidden="true">↗</span></button></div>
    <div className="first-batch-grid">{systems.map((system) => <article className="first-batch-card mini-system-card" key={system.id}>
      <a className="first-batch-visual mini-system-visual" href={productHref(system.sku)} aria-label={`View ${system.name} product page`}><img src={images[system.sku]} alt={`${system.name} JON.PC mini PC preview`} /></a>
      <div className="first-batch-card-body">
        <div className="first-batch-card-top"><span>{system.available && system.saleEnabled ? "In stock" : "Unavailable"}</span></div>
        <h3><a href={productHref(system.sku)}>{system.name}</a></h3>
        <p>{system.recommendedFor}</p>
        <div className="first-batch-card-bottom">
          <strong>{money(system.priceCents)} <small>incl. GST</small></strong>
          <a className="first-batch-configure" href={productHref(system.sku)}>View details <span aria-hidden="true">↗</span></a>
        </div>
      </div>
    </article>)}</div>

    {compareOpen && <div className="first-batch-overlay" onMouseDown={(event) => event.target === event.currentTarget && closeComparePanel()}><section className="first-batch-dialog first-batch-compare" role="dialog" aria-modal="true" aria-labelledby="mini-system-compare-title"><button className="first-batch-close" type="button" onClick={closeComparePanel} aria-label="Close">×</button><span className="section-kicker">JON.PC / Compact comparison</span><h3 id="mini-system-compare-title">Choose your edge.</h3><p>Compare compact systems by compute, memory and local-AI scale.</p><span className="first-batch-compare-hint">Swipe to compare <span aria-hidden="true">→</span></span><div className="first-batch-compare-scroll"><table><thead><tr><th>Difference</th>{systems.map((system) => <th key={system.id}>{system.name}</th>)}</tr></thead><tbody>{["Processor", "AI acceleration", "Memory", "Storage", "Networking"].map((key) => <tr key={key}><th>{key}</th>{systems.map((system) => <td key={system.id}>{system.specifications[key]}</td>)}</tr>)}<tr><th>Designed for</th>{systems.map((system) => <td key={system.id}>{system.recommendedFor}</td>)}</tr><tr><th>Price incl. GST</th>{systems.map((system) => <td key={system.id}>{money(system.priceCents)}</td>)}</tr></tbody></table></div></section></div>}
  </div>;
}

export default MiniSystemsCatalog;
