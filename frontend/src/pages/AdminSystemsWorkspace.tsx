import { useEffect, useState, type FormEvent } from "react";
import { adjustSystemStock, createAdminSystem, fetchAdminSystems, receiveSystemStock, updateAdminSystem, type SystemProduct, type SystemProductInput } from "../api/admin";
import "./AdminSystemsWorkspace.css";

const baseSpecifications = ["GPU", "CPU", "Memory", "Storage", "Cooler", "Motherboard", "PSU", "Case", "Fans", "Networking", "Accessories", "OS", "Warranty"];

function AdminSystemsWorkspace({ refreshKey }: { refreshKey: number }) {
  const [systems, setSystems] = useState<SystemProduct[]>([]);
  const [editor, setEditor] = useState<SystemProduct | "new" | null>(null);
  const [stockEditor, setStockEditor] = useState<{ mode: "receipt" | "adjustment"; system: SystemProduct } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      setSystems(await fetchAdminSystems());
      setError(null);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load systems");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let active = true;
    void fetchAdminSystems().then((next) => {
      if (!active) return;
      setSystems(next);
      setError(null);
    }).catch((requestError) => {
      if (active) setError(requestError instanceof Error ? requestError.message : "Unable to load systems");
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [refreshKey]);

  return <section className="admin-systems-workspace">
    <div className="admin-systems-heading"><div><span>Inventory / Systems</span><h3>Whole systems.</h3></div><button className="inventory-primary-button" type="button" onClick={() => setEditor("new")}>Add system</button></div>
    {error && <div className="admin-error" role="alert">{error}</div>}
    {loading && !systems.length ? <div className="admin-empty">Loading systems</div> : <div className="admin-table-wrap"><table><thead><tr><th>System</th><th>Price incl. GST</th><th>On hand</th><th>Available</th><th>Preview</th><th>Stock</th><th /></tr></thead><tbody>{systems.map((system) => <tr key={system.id}><td><strong>{system.name}</strong><small>{system.sku} · {system.productRange} · {system.salesMode === "PREORDER" ? "Pre-order" : system.salesMode === "STOCK_CHECK_REQUIRED" ? "Stock check required" : "Standard"}</small></td><td>${(system.priceCents / 100).toLocaleString("en-AU")}</td><td>{system.onHandQuantity}</td><td className={system.availableQuantity <= 1 ? "admin-system-stock-alert" : ""}>{system.availableQuantity <= 0 ? "Sold out" : system.availableQuantity}</td><td>{system.previewEnabled ? "On site" : "Admin only"}<small>{system.status}</small></td><td><div className="admin-system-stock-actions"><button className="inventory-row-link" type="button" onClick={() => setStockEditor({ mode: "receipt", system })}>Receive</button><button className="inventory-row-link" type="button" onClick={() => setStockEditor({ mode: "adjustment", system })}>Adjust</button></div></td><td><button className="inventory-row-link" type="button" onClick={() => setEditor(system)}>Edit →</button></td></tr>)}</tbody></table>{!systems.length && !loading && <div className="admin-empty">No system drafts yet</div>}</div>}
    {editor && <SystemEditor system={editor === "new" ? null : editor} onClose={() => setEditor(null)} onSaved={async () => { setEditor(null); await load(); }} />}
    {stockEditor && <SystemStockEditor {...stockEditor} onClose={() => setStockEditor(null)} onSaved={async () => { setStockEditor(null); await load(); }} />}
  </section>;
}

function SystemEditor({ system, onClose, onSaved }: { system: SystemProduct | null; onClose: () => void; onSaved: () => Promise<void> }) {
  const [draft, setDraft] = useState<SystemProductInput>(() => system ? {
    sku: system.sku, name: system.name, description: system.description ?? "", priceCents: system.priceCents,
    plannedPreorderQuantity: system.plannedPreorderQuantity, listingOrder: system.listingOrder,
    productRange: system.productRange, salesMode: system.salesMode,
    badge: system.badge, recommendedFor: system.recommendedFor, dispatchEstimate: system.dispatchEstimate,
    specifications: system.specifications, previewEnabled: system.previewEnabled,
  } : { sku: "", name: "", description: "", priceCents: 0, plannedPreorderQuantity: 0, listingOrder: 5, productRange: "Unassigned", salesMode: "STANDARD", badge: null, recommendedFor: "", dispatchEstimate: "", specifications: Object.fromEntries(baseSpecifications.map((key) => [key, ""])), previewEnabled: false });
  const [price, setPrice] = useState(system ? (system.priceCents / 100).toFixed(2) : "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const input = { ...draft, priceCents: Math.round(Number(price) * 100), specifications: Object.fromEntries(Object.entries(draft.specifications).filter(([key, value]) => key.trim() && value.trim())) };
      if (system) await updateAdminSystem(system.id, input);
      else await createAdminSystem(input);
      await onSaved();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to save system");
    } finally {
      setBusy(false);
    }
  }

  return <div className="admin-systems-overlay" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="admin-systems-drawer" role="dialog" aria-modal="true" aria-labelledby="system-editor-title"><button className="admin-systems-close" type="button" onClick={onClose} aria-label="Close">×</button><span className="section-kicker">Inventory / System catalogue</span><h3 id="system-editor-title">{system ? "Edit system" : "Add system"}</h3><form onSubmit={submit} className="admin-systems-form"><div className="admin-systems-form-grid"><label>System SKU<input required value={draft.sku} onChange={(event) => setDraft({ ...draft, sku: event.target.value.toUpperCase() })} placeholder="JON-STK-5060" /></label><label>Display order<input type="number" min="0" required value={draft.listingOrder} onChange={(event) => setDraft({ ...draft, listingOrder: Number(event.target.value) })} /></label></div><label>Product name<input required value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} /></label><div className="admin-systems-form-grid"><label>Product range<input required value={draft.productRange} onChange={(event) => setDraft({ ...draft, productRange: event.target.value })} /></label><label>Sales mode<select value={draft.salesMode} onChange={(event) => setDraft({ ...draft, salesMode: event.target.value as SystemProductInput["salesMode"] })}><option value="STANDARD">Standard</option><option value="PREORDER">Pre-order</option><option value="STOCK_CHECK_REQUIRED">Stock check required</option></select></label></div><label>Short description<input value={draft.description ?? ""} onChange={(event) => setDraft({ ...draft, description: event.target.value })} /></label><label>Price incl. GST (AUD)<input type="text" inputMode="decimal" pattern="[0-9]+([.][0-9]{1,2})?" required value={price} onChange={(event) => /^\d*(?:\.\d{0,2})?$/.test(event.target.value) && setPrice(event.target.value)} onBlur={() => price && setPrice(Number(price).toFixed(2))} /></label><div className="admin-systems-form-grid"><label>Recommended for<input value={draft.recommendedFor ?? ""} onChange={(event) => setDraft({ ...draft, recommendedFor: event.target.value })} /></label><label>Estimated dispatch<input value={draft.dispatchEstimate ?? ""} onChange={(event) => setDraft({ ...draft, dispatchEstimate: event.target.value })} /></label></div><label>Badge<input value={draft.badge ?? ""} onChange={(event) => setDraft({ ...draft, badge: event.target.value || null })} placeholder="Optional" /></label><label className="admin-systems-check"><input type="checkbox" checked={draft.previewEnabled} onChange={(event) => setDraft({ ...draft, previewEnabled: event.target.checked })} />Show read-only preview on site</label><details className="admin-systems-specs"><summary>Technical specifications</summary>{Object.entries(draft.specifications).map(([key, value]) => <label key={key}>{key}<input value={value} onChange={(event) => setDraft({ ...draft, specifications: { ...draft.specifications, [key]: event.target.value } })} /></label>)}</details>{error && <p className="admin-error" role="alert">{error}</p>}<button className="inventory-submit" type="submit" disabled={busy}>{busy ? "Saving…" : "Save draft"}<span>→</span></button></form></section></div>;
}

function SystemStockEditor({ mode, system, onClose, onSaved }: { mode: "receipt" | "adjustment"; system: SystemProduct; onClose: () => void; onSaved: () => Promise<void> }) {
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(null);
    try {
      const input = { quantity: Number(quantity), reason, idempotencyKey: crypto.randomUUID() };
      if (mode === "receipt") await receiveSystemStock(system.id, input);
      else await adjustSystemStock(system.id, input);
      await onSaved();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to update stock");
    } finally { setBusy(false); }
  }
  return <div className="admin-systems-overlay" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="admin-systems-drawer" role="dialog" aria-modal="true" aria-labelledby="system-stock-title"><button className="admin-systems-close" type="button" onClick={onClose} aria-label="Close">×</button><span className="section-kicker">Inventory / Whole systems</span><h3 id="system-stock-title">{mode === "receipt" ? "Receive systems" : "Adjust stock"}</h3><form onSubmit={submit} className="admin-systems-form"><div className="admin-system-stock-summary"><strong>{system.name}</strong><span>{system.onHandQuantity} on hand · {system.availableQuantity} available</span></div><label>{mode === "receipt" ? "Quantity received" : "Quantity change"}<input type="number" min={mode === "receipt" ? 1 : undefined} step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} placeholder={mode === "receipt" ? "2" : "Use -1 to remove one"} required /></label><label>{mode === "receipt" ? "Delivery reference" : "Reason for adjustment"}<input value={reason} onChange={(event) => setReason(event.target.value)} placeholder={mode === "receipt" ? "Purchase order or supplier invoice" : "Stock count correction or damaged system"} required /></label>{error && <p className="admin-error" role="alert">{error}</p>}<button className="inventory-submit" type="submit" disabled={busy}>{busy ? "Recording…" : mode === "receipt" ? "Confirm systems received" : "Confirm adjustment"}<span>→</span></button></form></section></div>;
}

export default AdminSystemsWorkspace;
