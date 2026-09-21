import { useEffect, useRef, useState } from "react";
import { createLocalSystemRequest, type PreviewSystem } from "../api/systems";
import { authTokenKey } from "../api/auth";
import strikeImage from "../assets/featured-systems/strike-5060.jpeg";
import strikeTiImage from "../assets/featured-systems/strike-ti.jpeg";
import stormImage from "../assets/featured-systems/storm-5070.jpeg";
import stormTiImage from "../assets/featured-systems/storm-ti.jpeg";
import "./FirstBatchCatalog.css";

const sharedSpecOrder = ["CPU", "Memory", "Storage", "Motherboard", "Cooler", "PSU", "Case", "Fans", "Networking", "Accessories", "OS"];

function money(cents: number) { return `$${(cents / 100).toLocaleString("en-AU")} AUD`; }
const systemImages = [strikeImage, strikeTiImage, stormImage, stormTiImage];

function FirstBatchCatalog({ systems }: { systems: PreviewSystem[] }) {
  const [selected, setSelected] = useState<PreviewSystem | null>(null);
  const [compareOpen, setCompareOpen] = useState(false);
  const [allSpecs, setAllSpecs] = useState(false);
  const [requestingSku, setRequestingSku] = useState<string | null>(null);
  const [requestError, setRequestError] = useState<string | null>(null);
  const resumeSkuAfterLogin = useRef<string | null>(null);
  const sorted = [...systems].sort((a, b) => a.priceCents - b.priceCents);
  const shared = sharedSpecOrder.filter((key) => sorted.every((system) => system.specifications[key] === sorted[0].specifications[key]));

  useEffect(() => {
    const resume = () => {
      const system = systems.find((item) => item.sku === resumeSkuAfterLogin.current);
      resumeSkuAfterLogin.current = null;
      if (system) void requestBuild(system);
    };
    window.addEventListener("jonpc:authenticated", resume);
    return () => window.removeEventListener("jonpc:authenticated", resume);
  }, [systems]);

  async function requestBuild(system: PreviewSystem) {
    if (!system.available) return;
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      resumeSkuAfterLogin.current = system.sku;
      window.dispatchEvent(new CustomEvent("jonpc:open-account"));
      return;
    }
    setRequestingSku(system.sku);
    setRequestError(null);
    try {
      const result = await createLocalSystemRequest(token, system.sku, system.priceCents);
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
      setSelected(null);
      window.dispatchEvent(new CustomEvent("jonpc:open-checkout", { detail: { requestReference: result.requestReference } }));
    } catch (error) {
      setRequestError(error instanceof Error ? error.message : "Unable to request this system.");
    } finally {
      setRequestingSku(null);
    }
  }

  return <section className="systems-section first-batch-section" id="systems" aria-labelledby="systems-title">
    <div className="systems-heading"><div><span className="section-kicker">JON. PC / Featured systems</span><h2 id="systems-title">Chosen by us.<br /><em>Built for your play.</em></h2></div><p>Ready-configured favourites. Find your level of play.</p></div>
    <div className="first-batch-toolbar"><div><span>JON. PC gaming range</span><strong>Strike → Storm</strong></div><button type="button" onClick={() => { setCompareOpen(true); setAllSpecs(false); }} aria-haspopup="dialog">Compare models <span aria-hidden="true">↗</span></button></div>
    <div className="first-batch-grid">{sorted.map((system, index) => <article className="first-batch-card" key={system.id}>
      <div className={`first-batch-visual first-batch-visual-${index + 1}`}><img src={systemImages[index % systemImages.length]} alt={`${system.name} illustrative PC preview`} /><span className="first-batch-visual-label">JON. PC / 0{index + 1} · Illustrative</span></div>
      <div className="first-batch-card-body"><div className="first-batch-card-top"><span>{system.available ? "Pre-order preview" : "Sold out"}</span></div><h3>{system.name.replace("JON PC ", "")}</h3><p>{system.recommendedFor}</p><div className="first-batch-card-bottom"><strong>{money(system.priceCents)} <small>incl. GST</small></strong><div className="first-batch-card-actions"><button type="button" onClick={() => setSelected(system)}>View system <span aria-hidden="true">↗</span></button>{import.meta.env.DEV && <button type="button" className="first-batch-request" onClick={() => void requestBuild(system)} disabled={requestingSku !== null || !system.available}>{!system.available ? "Sold out" : requestingSku === system.sku ? "Preparing..." : "Request build →"}</button>}</div></div></div>
    </article>)}</div>
    {requestError && <p className="first-batch-error" role="alert">{requestError}</p>}
    {selected && <div className="first-batch-overlay" onMouseDown={(event) => event.target === event.currentTarget && setSelected(null)}><section className="first-batch-dialog" role="dialog" aria-modal="true" aria-labelledby="first-batch-detail-title"><button className="first-batch-close" type="button" onClick={() => setSelected(null)} aria-label="Close">×</button><span className="section-kicker">JON. PC / System detail</span><h3 id="first-batch-detail-title">{selected.name.replace("JON PC ", "")}</h3><p>{selected.description}</p><div className="first-batch-detail-highlights"><div><span>Graphics</span><strong>{selected.specifications.GPU}</strong></div><div><span>Designed for</span><strong>{selected.recommendedFor}</strong></div><div><span>Price incl. GST</span><strong>{money(selected.priceCents)}</strong></div></div><div className="first-batch-detail-dispatch"><span>Pre-order</span><strong>Estimated dispatch: {selected.dispatchEstimate}</strong></div><details className="first-batch-full-specs"><summary>Full specifications</summary>{Object.entries(selected.specifications).map(([key, value]) => <div key={key}><span>{key}</span><strong>{value}</strong></div>)}</details>{requestError && <p className="first-batch-error" role="alert">{requestError}</p>}{import.meta.env.DEV && <button className="first-batch-advice" type="button" onClick={() => void requestBuild(systems.find((item) => item.id === selected.id) ?? selected)} disabled={requestingSku !== null || !(systems.find((item) => item.id === selected.id)?.available ?? selected.available)}>{!(systems.find((item) => item.id === selected.id)?.available ?? selected.available) ? "Sold out" : requestingSku === selected.sku ? "Preparing..." : "Request build"} <span aria-hidden="true">↗</span></button>}<button className="first-batch-advice first-batch-advice-secondary" type="button" onClick={() => { setSelected(null); window.dispatchEvent(new CustomEvent("jonpc:open-ai")); }}>Questions? Ask JON.AI <span aria-hidden="true">↗</span></button></section></div>}
    {compareOpen && <div className="first-batch-overlay" onMouseDown={(event) => event.target === event.currentTarget && setCompareOpen(false)}><section className="first-batch-dialog first-batch-compare" role="dialog" aria-modal="true" aria-labelledby="first-batch-compare-title"><button className="first-batch-close" type="button" onClick={() => setCompareOpen(false)} aria-label="Close">×</button><span className="section-kicker">JON. PC / Compare</span><h3 id="first-batch-compare-title">Choose your graphics.</h3><p>Same core platform. Find the level that fits your play.</p><div className="first-batch-compare-scroll"><table><thead><tr><th>Difference</th>{sorted.map((system) => <th key={system.id}>{system.name.replace("JON PC ", "")}</th>)}</tr></thead><tbody><tr><th>Graphics</th>{sorted.map((system) => <td key={system.id}>{system.specifications.GPU}</td>)}</tr><tr><th>Designed for</th>{sorted.map((system) => <td key={system.id}>{system.recommendedFor}</td>)}</tr><tr><th>Price incl. GST</th>{sorted.map((system) => <td key={system.id}>{money(system.priceCents)}</td>)}</tr></tbody></table></div><button className="first-batch-expand" type="button" onClick={() => setAllSpecs(!allSpecs)} aria-expanded={allSpecs}>{allSpecs ? "Hide shared specifications" : "Show shared specifications"} <span aria-hidden="true">{allSpecs ? "−" : "+"}</span></button>{allSpecs && <div className="first-batch-shared">{shared.map((key) => <div key={key}><span>{key}</span><strong>{sorted[0].specifications[key]}</strong></div>)}</div>}</section></div>}
  </section>;
}

export default FirstBatchCatalog;
