import { useEffect, useRef, useState } from "react";
import edgeMiniImage from "../assets/minipc/edge-mini.jpg";
import edgeMiniExplodedImage from "../assets/minipc/edge-mini-exploded.jpg";
import edgeAiImage from "../assets/minipc/edge-ai.jpg";
import edgeProImage from "../assets/minipc/edge-pro.jpg";
import edgeMaxImage from "../assets/minipc/edge-max.jpg";
import { miniSystems, type MiniSystem } from "../data/miniSystems";
import { useHistoryPanel } from "../hooks/useHistoryPanel";

const images = {
  edgeMini: edgeMiniImage,
  edgeMiniExploded: edgeMiniExplodedImage,
  edgeAi: edgeAiImage,
  edgePro: edgeProImage,
  edgeMax: edgeMaxImage,
};

function money(cents: number) { return `$${(cents / 100).toLocaleString("en-AU")} AUD`; }

function MiniSystemsCatalog() {
  const [selected, setSelected] = useState<MiniSystem | null>(null);
  const [detailHistoryOpen, setDetailHistoryOpen] = useState(false);
  const [compareOpen, setCompareOpen] = useState(false);
  const lastSelected = useRef<MiniSystem | null>(null);
  const { open: openDetailPanel, close: closeDetailPanel } = useHistoryPanel("mini-system-detail", detailHistoryOpen, setDetailHistoryOpen);
  const { open: openComparePanel, close: closeComparePanel } = useHistoryPanel("mini-system-compare", compareOpen, setCompareOpen);

  useEffect(() => {
    if (detailHistoryOpen && !selected && lastSelected.current) setSelected(lastSelected.current);
    if (!detailHistoryOpen && selected) setSelected(null);
  }, [detailHistoryOpen, selected]);

  function showSystem(system: MiniSystem) {
    lastSelected.current = system;
    setSelected(system);
    openDetailPanel();
  }

  return <div className="mini-systems-catalog" id="compact-ai-systems">
    <div className="first-batch-toolbar first-batch-toolbar-mini"><div><span>JON.PC compact AI systems</span><strong>Local AI → Edge compute</strong></div><button type="button" onClick={openComparePanel} aria-haspopup="dialog">Compare mini PCs <span aria-hidden="true">↗</span></button></div>
    <div className="first-batch-grid">{miniSystems.map((system, index) => <article className="first-batch-card mini-system-card" key={system.id}>
      <div className="first-batch-visual mini-system-visual"><img src={images[system.image as keyof typeof images]} alt={`${system.name} illustrative JON.PC mini PC`} /><span className="first-batch-visual-label">JON.PC / 0{index + 1} · Illustrative</span></div>
      <div className="first-batch-card-body"><div className="first-batch-card-top"><span>Availability review</span></div><h3>{system.name}</h3><p>{system.recommendedFor}</p><div className="first-batch-card-bottom"><strong>{money(system.priceCents)} <small>indicative · incl. GST</small></strong><div className="first-batch-card-actions"><button type="button" onClick={() => showSystem(system)}>View system <span aria-hidden="true">↗</span></button></div></div></div>
    </article>)}</div>
    <p className="first-batch-footnote">Illustrative JON.PC concepts with indicative Australian pricing. Final specification, price and availability require confirmation.</p>

    {selected && <div className="first-batch-overlay" onMouseDown={(event) => event.target === event.currentTarget && closeDetailPanel()}><section className="first-batch-dialog" role="dialog" aria-modal="true" aria-labelledby="mini-system-detail-title"><button className="first-batch-close" type="button" onClick={closeDetailPanel} aria-label="Close">×</button><span className="section-kicker">JON.PC / Compact system</span><h3 id="mini-system-detail-title">{selected.name}</h3><p>{selected.description}</p><div className="mini-system-detail-image"><img src={images[(selected.detailImage ?? selected.image) as keyof typeof images]} alt={`${selected.name} illustrative product detail`} /></div><div className="first-batch-detail-highlights"><div><span>Processor</span><strong>{selected.specifications.Processor}</strong></div><div><span>Designed for</span><strong>{selected.recommendedFor}</strong></div><div><span>Indicative price</span><strong>{money(selected.priceCents)} incl. GST</strong></div></div><div className="first-batch-detail-dispatch"><span>Availability</span><strong>{selected.availability}</strong></div><details className="first-batch-full-specs"><summary>Full specifications</summary>{Object.entries(selected.specifications).map(([key, value]) => <div key={key}><span>{key}</span><strong>{value}</strong></div>)}</details><a className="first-batch-advice mini-system-enquiry" href={`mailto:support@jonpc.com.au?subject=${encodeURIComponent(`JON.PC ${selected.name} availability`)}`}>Request availability <span aria-hidden="true">↗</span></a></section></div>}

    {compareOpen && <div className="first-batch-overlay" onMouseDown={(event) => event.target === event.currentTarget && closeComparePanel()}><section className="first-batch-dialog first-batch-compare" role="dialog" aria-modal="true" aria-labelledby="mini-system-compare-title"><button className="first-batch-close" type="button" onClick={closeComparePanel} aria-label="Close">×</button><span className="section-kicker">JON.PC / Compact comparison</span><h3 id="mini-system-compare-title">Choose your edge.</h3><p>Compare compact systems by compute, memory and local-AI scale.</p><span className="first-batch-compare-hint">Swipe to compare <span aria-hidden="true">→</span></span><div className="first-batch-compare-scroll"><table><thead><tr><th>Difference</th>{miniSystems.map((system) => <th key={system.id}>{system.name}</th>)}</tr></thead><tbody>{["Processor", "AI acceleration", "Memory", "Storage", "Networking"].map((key) => <tr key={key}><th>{key}</th>{miniSystems.map((system) => <td key={system.id}>{system.specifications[key]}</td>)}</tr>)}<tr><th>Designed for</th>{miniSystems.map((system) => <td key={system.id}>{system.recommendedFor}</td>)}</tr><tr><th>Indicative price</th>{miniSystems.map((system) => <td key={system.id}>{money(system.priceCents)}</td>)}</tr></tbody></table></div><p className="first-batch-footnote">Specifications and pricing are illustrative until the final product range is confirmed.</p></section></div>}
  </div>;
}

export default MiniSystemsCatalog;
