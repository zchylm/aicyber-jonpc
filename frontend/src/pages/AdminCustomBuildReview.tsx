import { useEffect, useState, type FormEvent } from "react";
import { fetchCustomBuildReviews, publishCustomBuildQuote, type CustomBuildReviewItem } from "../api/admin";
import { fetchConfiguratorCatalog, type ConfiguratorCatalog } from "../api/configurator";
import "./AdminCustomBuildReview.css";

const fields = ["cpuId", "gpuId", "memoryId", "storageId", "motherboardId", "psuId", "caseId", "coolingId"];
const names: Record<string, string> = { cpuId: "Processor", gpuId: "Graphics", memoryId: "Memory", storageId: "Storage", motherboardId: "Motherboard", psuId: "Power supply", caseId: "Case", coolingId: "Cooling" };

function AdminCustomBuildReview({ refreshKey }: { refreshKey: number }) {
  const [items, setItems] = useState<CustomBuildReviewItem[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [price, setPrice] = useState("");
  const [note, setNote] = useState("");
  const [catalog, setCatalog] = useState<ConfiguratorCatalog | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    fetchCustomBuildReviews().then(setItems).catch((error: unknown) => setMessage(error instanceof Error ? error.message : "Unable to load reviews"));
    fetchConfiguratorCatalog().then(setCatalog).catch(() => {});
  }, [refreshKey]);

  const selected = items.find((item) => item.id === selectedId) ?? null;
  const filtered = items.filter((item) => `${item.requestReference} ${item.name} ${item.email}`.toLowerCase().includes(query.toLowerCase()));

  function open(item: CustomBuildReviewItem) {
    setSelectedId(item.id);
    setPrice(item.quoteStatus === "SENT" ? (item.totalCents / 100).toFixed(2) : "");
    setNote(item.reviewNote ?? "");
    setMessage(null);
  }

  async function publish(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    if (!/^\d+(?:\.\d{1,2})?$/.test(price) || Number(price) <= 0) { setMessage("Enter a valid AUD amount."); return; }
    setBusy(true);
    setMessage(null);
    try {
      const updated = await publishCustomBuildQuote(selected.id, {
        totalCents: Math.round(Number(price) * 100), reviewNote: note.trim(),
        validUntil: null,
      });
      setItems((current) => current.map((item) => item.id === updated.id ? updated : item));
      setMessage("Final quote is ready for the customer to review in My orders.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Unable to publish quote");
    } finally { setBusy(false); }
  }

  return <div className="custom-review-workspace">
    <section className="admin-section-heading compact"><span>{items.filter((item) => !item.quoteStatus).length} awaiting review</span><h2>Custom Build Review.</h2></section>
    <div className="custom-review-search"><label>Find a request<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Reference, customer or email" /></label></div>
    <div className="admin-table-wrap"><table><thead><tr><th>Request</th><th>Customer</th><th>Direction</th><th>Estimate</th><th>Review</th><th>Submitted</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id} onClick={() => open(item)}><td><strong>{item.requestReference}</strong></td><td><strong>{item.name}</strong><small>{item.email}</small></td><td>{item.direction}</td><td>${item.estimatedPrice.toLocaleString("en-AU")}</td><td><span className={`custom-review-status ${item.quoteStatus?.toLowerCase() ?? "pending"}`}>{item.quoteStatus === "SENT" ? "Quote ready" : item.quoteStatus === "ACCEPTED" ? "Accepted" : "Review needed"}</span></td><td>{new Date(item.createdAt).toLocaleDateString("en-AU")}</td></tr>)}</tbody></table>{filtered.length === 0 && <div className="admin-empty">No matching custom requests.</div>}</div>
    {selected && <div className="admin-drawer-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setSelectedId(null)}><aside className="admin-drawer custom-review-drawer" aria-label={`Review ${selected.requestReference}`}><button className="admin-drawer-close" type="button" onClick={() => setSelectedId(null)}>×</button><span>Custom Build Review</span><h2>{selected.requestReference}</h2><div className="custom-review-summary"><div><small>Customer</small><strong>{selected.name}</strong><p>{selected.email}{selected.phone && <> · {selected.phone}</>}</p></div><div><small>Customer estimate</small><strong>${selected.estimatedPrice.toLocaleString("en-AU")}</strong><p>Not a final price</p></div></div><h3>Selected configuration</h3><dl className="custom-review-specs">{fields.map((field) => { const id = String(selected.configuration[field] ?? ""); const label = catalog?.options[field.replace(/Id$/, "")]?.find((option) => option.id === id)?.label ?? id; return <div key={field}><dt>{names[field]}</dt><dd>{label || "—"}</dd></div>; })}</dl><h3>Customer priorities</h3><dl className="custom-review-specs">{Object.entries(selected.configuration.answers ?? {}).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value}</dd></div>)}</dl>{selected.notes && <div className="custom-review-customer-note"><h3>Customer note</h3><p>{selected.notes}</p></div>}{selected.quoteStatus === "ACCEPTED" ? <div className="custom-review-accepted"><strong>Quote accepted</strong><p>${(selected.totalCents / 100).toLocaleString("en-AU")} incl. GST · Version {selected.quoteVersion}</p></div> : <form className="custom-review-form" onSubmit={(event) => void publish(event)}><h3>{selected.quoteStatus === "SENT" ? "Revise final quote" : "Publish final quote"}</h3><label>Final total (AUD, incl. GST)<input value={price} onChange={(event) => setPrice(event.target.value)} inputMode="decimal" placeholder="2499.00" required /></label><label>Review note <small>Optional</small><textarea value={note} onChange={(event) => setNote(event.target.value)} rows={3} placeholder="Availability or delivery notes" /></label>{message && <p role="status">{message}</p>}<button type="submit" disabled={busy}>{busy ? "Publishing…" : "Send quote to customer →"}</button></form>}</aside></div>}
  </div>;
}

export default AdminCustomBuildReview;
