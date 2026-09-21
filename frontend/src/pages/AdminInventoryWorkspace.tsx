import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  adjustInventory,
  createInventoryItem,
  createInventoryLocation,
  fetchAdminInventory,
  receiveInventory,
  updateInventoryItem,
  type AdminInventory,
  type AdminInventoryItem,
  type InventoryItemInput,
} from "../api/admin";
import "./AdminInventoryWorkspace.css";
import AdminSystemsWorkspace from "./AdminSystemsWorkspace";

type InventoryView = "stock" | "catalogue" | "movements" | "locations" | "systems";
type StockMode = "receipt" | "adjustment";
type Specification = { key: string; value: string };

const emptyItem = {
  categoryCode: "CPU",
  sku: "",
  brand: "",
  model: "",
  displayName: "",
  description: "",
  unitCost: "",
  retailPrice: "",
  status: "ACTIVE",
};

function AdminInventoryWorkspace({ refreshKey }: { refreshKey: number }) {
  const [inventory, setInventory] = useState<AdminInventory | null>(null);
  const [view, setView] = useState<InventoryView>("stock");
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [itemEditor, setItemEditor] = useState<AdminInventoryItem | "new" | null>(null);
  const [stockEditor, setStockEditor] = useState<{ mode: StockMode; item: AdminInventoryItem } | null>(null);
  const [locationEditor, setLocationEditor] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setInventory(await fetchAdminInventory(query, category, status));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load inventory");
    } finally {
      setLoading(false);
    }
  }, [category, query, status]);

  useEffect(() => {
    const loadId = window.setTimeout(() => void load(), query ? 250 : 0);
    return () => window.clearTimeout(loadId);
  }, [load, query, refreshKey]);

  const completed = async (message: string) => {
    setNotice(message);
    await load();
    window.setTimeout(() => setNotice(null), 2800);
  };

  if (!inventory && loading) return <div className="admin-empty">Loading inventory</div>;

  return (
    <>
      <section className="admin-section-heading compact inventory-heading">
        <div><span>Inventory control</span><h2>JON. PC Stock.</h2></div>
        <div className="inventory-heading-actions">
          {view !== "systems" && <><button className="inventory-secondary-button" type="button" onClick={() => inventory?.items[0] && setStockEditor({ mode: "receipt", item: inventory.items[0] })} disabled={!inventory?.items.length || !inventory?.locations.length}>Receive stock</button>
          <button className="inventory-primary-button" type="button" onClick={() => setItemEditor("new")}>Add component</button></>}
        </div>
      </section>

      {error && <div className="admin-error" role="alert">{error}</div>}
      {notice && <div className="inventory-notice" role="status">{notice}</div>}

      {inventory && <InventorySummary inventory={inventory} />}

      <div className="admin-subnav inventory-subnav">
        {(["stock", "catalogue", "movements", "locations", "systems"] as InventoryView[]).map((item) => (
          <button className={view === item ? "active" : ""} type="button" onClick={() => setView(item)} key={item}>{inventoryViewLabel(item)}</button>
        ))}
      </div>

      {(view === "stock" || view === "catalogue") && inventory && (
        <>
          <div className="admin-filters inventory-filters">
            <label><span>Search</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="SKU, component, brand or model" /></label>
            <label><span>Category</span><select value={category} onChange={(event) => setCategory(event.target.value)}><option value="">All categories</option>{inventory.categories.map((item) => <option value={item.code} key={item.id}>{item.name}</option>)}</select></label>
            <label><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All statuses</option><option value="ACTIVE">Active — in use</option><option value="DRAFT">Draft — not ready</option><option value="DISCONTINUED">Discontinued — retired</option></select></label>
          </div>
          {view === "stock"
            ? <StockTable inventory={inventory} onReceive={(item) => setStockEditor({ mode: "receipt", item })} onAdjust={(item) => setStockEditor({ mode: "adjustment", item })} />
            : <CatalogueTable inventory={inventory} onEdit={setItemEditor} />}
        </>
      )}

      {view === "movements" && inventory && <MovementTable inventory={inventory} />}
      {view === "locations" && inventory && <LocationTable inventory={inventory} onAdd={() => setLocationEditor(true)} />}
      {view === "systems" && <AdminSystemsWorkspace refreshKey={refreshKey} />}

      {itemEditor && inventory && (
        <ItemDrawer
          item={itemEditor === "new" ? null : itemEditor}
          categories={inventory.categories}
          onClose={() => setItemEditor(null)}
          onComplete={async (message) => { setItemEditor(null); await completed(message); }}
        />
      )}
      {stockEditor && inventory && (
        <StockDrawer
          mode={stockEditor.mode}
          initialItem={stockEditor.item}
          inventory={inventory}
          onClose={() => setStockEditor(null)}
          onComplete={async (message) => { setStockEditor(null); await completed(message); }}
        />
      )}
      {locationEditor && <LocationDrawer onClose={() => setLocationEditor(false)} onComplete={async (message) => { setLocationEditor(false); await completed(message); }} />}
    </>
  );
}

function InventorySummary({ inventory }: { inventory: AdminInventory }) {
  const cards = [
    ["Component SKUs", inventory.summary.totalSkus],
    ["Units in stock", inventory.summary.totalOnHand],
    ["Reserved", inventory.summary.totalReserved],
    ["Stock value at cost", money(inventory.summary.inventoryCostCents)],
  ];
  return <div className="inventory-summary">{cards.map(([name, value]) => <div key={name}><span>{name}</span><strong>{value}</strong></div>)}</div>;
}

function StockTable({ inventory, onReceive, onAdjust }: { inventory: AdminInventory; onReceive: (item: AdminInventoryItem) => void; onAdjust: (item: AdminInventoryItem) => void }) {
  return <div className="admin-table-wrap"><table><thead><tr><th>Component</th><th>Category</th><th>In stock</th><th>Reserved</th><th>Available</th><th>Actions</th></tr></thead><tbody>{inventory.items.map((item) => <tr key={item.id}><td><strong>{item.displayName}</strong><small>{item.sku}</small></td><td>{item.categoryName}</td><td>{item.onHandQuantity}</td><td>{item.reservedQuantity}</td><td><b className={item.availableQuantity === 0 ? "inventory-zero" : "inventory-available"}>{item.availableQuantity}</b></td><td><div className="inventory-table-actions"><button type="button" onClick={() => onReceive(item)}>Receive stock</button><button type="button" onClick={() => onAdjust(item)}>Adjust stock</button></div></td></tr>)}</tbody></table>{inventory.items.length === 0 && <div className="admin-empty">No matching components</div>}</div>;
}

function CatalogueTable({ inventory, onEdit }: { inventory: AdminInventory; onEdit: (item: AdminInventoryItem) => void }) {
  return <div className="admin-table-wrap"><table><thead><tr><th>SKU</th><th>Component</th><th>Category</th><th>Unit cost</th><th>Selling price</th><th>Status</th><th /></tr></thead><tbody>{inventory.items.map((item) => <tr key={item.id}><td><strong>{item.sku}</strong></td><td>{item.displayName}<small>{item.brand} / {item.model}</small></td><td>{item.categoryName}</td><td>{item.unitCostCents === null ? "—" : money(item.unitCostCents)}</td><td>{money(item.retailPriceCents)}</td><td><Status value={item.status} /></td><td><button className="inventory-row-link" type="button" onClick={() => onEdit(item)}>Edit →</button></td></tr>)}</tbody></table>{inventory.items.length === 0 && <div className="admin-empty">No matching components</div>}</div>;
}

function MovementTable({ inventory }: { inventory: AdminInventory }) {
  return <div className="admin-table-wrap"><table><thead><tr><th>Time</th><th>Component</th><th>Location</th><th>Action</th><th>Change</th><th>Stock after</th><th>Recorded by</th></tr></thead><tbody>{inventory.recentMovements.map((movement) => <tr key={movement.id}><td>{dateTime(movement.createdAt)}</td><td><strong>{movement.itemName}</strong><small>{movement.sku}</small></td><td>{movement.locationCode}</td><td><Status value={movement.movementType} /><small>{movement.reason ?? "—"}</small></td><td><b className={movement.onHandDelta >= 0 ? "inventory-positive" : "inventory-negative"}>{signed(movement.onHandDelta)}</b>{movement.reservedDelta !== 0 && <small>{signed(movement.reservedDelta)} reserved</small>}</td><td>{movement.onHandAfter}<small>{movement.reservedAfter} reserved</small></td><td>{movement.performedBy ?? "System"}</td></tr>)}</tbody></table>{inventory.recentMovements.length === 0 && <div className="admin-empty">No stock history yet</div>}</div>;
}

function LocationTable({ inventory, onAdd }: { inventory: AdminInventory; onAdd: () => void }) {
  return <><div className="inventory-section-action"><span>{inventory.locations.length} locations</span><button className="inventory-primary-button" type="button" onClick={onAdd}>Add location</button></div><div className="admin-table-wrap"><table><thead><tr><th>Location</th><th>Type</th><th>On hand</th><th>Reserved</th><th>Available</th><th>Status</th></tr></thead><tbody>{inventory.locations.map((location) => <tr key={location.id}><td><strong>{location.name}</strong><small>{location.code}</small></td><td>{label(location.locationType)}</td><td>{location.onHandQuantity}</td><td>{location.reservedQuantity}</td><td>{location.onHandQuantity - location.reservedQuantity}</td><td><Status value={location.status} /></td></tr>)}</tbody></table>{inventory.locations.length === 0 && <div className="admin-empty">Add your first stock location</div>}</div></>;
}

function ItemDrawer({ item, categories, onClose, onComplete }: { item: AdminInventoryItem | null; categories: AdminInventory["categories"]; onClose: () => void; onComplete: (message: string) => Promise<void> }) {
  const [draft, setDraft] = useState(() => item ? {
    categoryCode: item.categoryCode, sku: item.sku, brand: item.brand, model: item.model,
    displayName: item.displayName, description: item.description ?? "",
    unitCost: item.unitCostCents === null ? "" : centsToDollars(item.unitCostCents),
    retailPrice: centsToDollars(item.retailPriceCents), status: item.status,
  } : { ...emptyItem, categoryCode: categories[0]?.code ?? "CPU" });
  const [specifications, setSpecifications] = useState<Specification[]>(() => {
    const entries = item ? Object.entries(item.specifications) : [];
    return entries.length ? entries.map(([key, value]) => ({ key, value: String(value) })) : [{ key: "", value: "" }];
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const input: InventoryItemInput = {
      categoryCode: draft.categoryCode,
      sku: draft.sku,
      brand: draft.brand,
      model: draft.model,
      displayName: draft.displayName,
      description: draft.description,
      specifications: Object.fromEntries(specifications.filter((entry) => entry.key.trim()).map((entry) => [entry.key.trim(), entry.value.trim()])),
      unitCostCents: draft.unitCost === "" ? null : dollarsToCents(draft.unitCost),
      retailPriceCents: dollarsToCents(draft.retailPrice),
      currency: "AUD",
      status: draft.status,
    };
    try {
      if (item) await updateInventoryItem(item.id, input);
      else await createInventoryItem(input);
      await onComplete(item ? "Component updated" : "Component added");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to save component");
    } finally {
      setBusy(false);
    }
  }

  return <Drawer title={item ? "Edit component" : "Add component"} kicker="Inventory / Component catalogue" onClose={onClose}><form className="inventory-form" onSubmit={submit}><div className="inventory-form-grid"><Field label="Category"><select value={draft.categoryCode} onChange={(event) => setDraft({ ...draft, categoryCode: event.target.value })}>{categories.map((category) => <option value={category.code} key={category.id}>{category.name}</option>)}</select></Field><Field label="SKU"><input value={draft.sku} onChange={(event) => setDraft({ ...draft, sku: event.target.value.toUpperCase() })} placeholder="AMD-R5-7600" required /></Field><Field label="Brand"><input value={draft.brand} onChange={(event) => setDraft({ ...draft, brand: event.target.value })} placeholder="AMD" required /></Field><Field label="Model"><input value={draft.model} onChange={(event) => setDraft({ ...draft, model: event.target.value })} placeholder="Ryzen 5 7600" required /></Field></div><Field label="Display name"><input value={draft.displayName} onChange={(event) => setDraft({ ...draft, displayName: event.target.value })} placeholder="AMD Ryzen 5 7600" required /></Field><Field label="Description" optional><textarea value={draft.description} onChange={(event) => setDraft({ ...draft, description: event.target.value })} rows={3} /></Field><div className="inventory-form-grid"><Field label="Unit cost (AUD)" optional><input type="text" inputMode="decimal" pattern="(?:[0-9]+|[0-9]*[.][0-9]{1,2})" placeholder="0.00" value={draft.unitCost} onChange={(event) => isMoneyInput(event.target.value) && setDraft({ ...draft, unitCost: event.target.value })} onBlur={() => draft.unitCost && setDraft({ ...draft, unitCost: formatMoneyInput(draft.unitCost) })} /></Field><Field label="Selling price (AUD)"><input type="text" inputMode="decimal" pattern="(?:[0-9]+|[0-9]*[.][0-9]{1,2})" placeholder="0.00" value={draft.retailPrice} onChange={(event) => isMoneyInput(event.target.value) && setDraft({ ...draft, retailPrice: event.target.value })} onBlur={() => draft.retailPrice && setDraft({ ...draft, retailPrice: formatMoneyInput(draft.retailPrice) })} required /></Field><Field label="Status"><select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value })}><option value="ACTIVE">Active — in use</option><option value="DRAFT">Draft — not ready</option><option value="DISCONTINUED">Discontinued — retired</option></select></Field></div><div className="inventory-specifications"><div><span>Technical specifications</span><button type="button" onClick={() => setSpecifications([...specifications, { key: "", value: "" }])}>Add specification +</button></div>{specifications.map((specification, index) => <div className="inventory-specification-row" key={index}><input aria-label="Specification name" placeholder="Socket" value={specification.key} onChange={(event) => setSpecifications(specifications.map((entry, entryIndex) => entryIndex === index ? { ...entry, key: event.target.value } : entry))} /><input aria-label="Specification value" placeholder="AM5" value={specification.value} onChange={(event) => setSpecifications(specifications.map((entry, entryIndex) => entryIndex === index ? { ...entry, value: event.target.value } : entry))} /><button type="button" aria-label="Remove specification" onClick={() => setSpecifications(specifications.filter((_, entryIndex) => entryIndex !== index))}>×</button></div>)}</div>{error && <p className="inventory-form-error" role="alert">{error}</p>}<button className="inventory-submit" type="submit" disabled={busy}>{busy ? "Saving…" : item ? "Save changes" : "Add component"}<span>→</span></button></form></Drawer>;
}

function StockDrawer({ mode, initialItem, inventory, onClose, onComplete }: { mode: StockMode; initialItem: AdminInventoryItem; inventory: AdminInventory; onClose: () => void; onComplete: (message: string) => Promise<void> }) {
  const [itemId, setItemId] = useState(initialItem.id);
  const [locationId, setLocationId] = useState(inventory.locations[0]?.id ?? "");
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const input = { inventoryItemId: itemId, locationId, quantity: Number(quantity), reason, idempotencyKey: crypto.randomUUID() };
      if (mode === "receipt") await receiveInventory(input);
      else await adjustInventory(input);
      await onComplete(mode === "receipt" ? "Stock received" : "Stock adjusted");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to update stock");
    } finally {
      setBusy(false);
    }
  }

  return <Drawer title={mode === "receipt" ? "Receive stock" : "Adjust stock"} kicker="Inventory / Stock" onClose={onClose}><form className="inventory-form" onSubmit={submit}><Field label={mode === "receipt" ? "Component received" : "Component"}><select value={itemId} onChange={(event) => setItemId(event.target.value)}>{inventory.items.map((item) => <option value={item.id} key={item.id}>{item.sku} · {item.displayName}</option>)}</select></Field><Field label="Stock location"><select value={locationId} onChange={(event) => setLocationId(event.target.value)} required>{inventory.locations.map((location) => <option value={location.id} key={location.id}>{location.code} · {location.name}</option>)}</select></Field><Field label={mode === "receipt" ? "Quantity received" : "Quantity change"}><input type="number" min={mode === "receipt" ? "1" : undefined} step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} placeholder={mode === "receipt" ? "10" : "Use -1 to remove one"} required /></Field><Field label={mode === "receipt" ? "Delivery reference" : "Reason for adjustment"}><input value={reason} onChange={(event) => setReason(event.target.value)} placeholder={mode === "receipt" ? "Purchase order, supplier invoice or delivery note" : "Stock count correction or damaged stock"} required /></Field>{error && <p className="inventory-form-error" role="alert">{error}</p>}<button className="inventory-submit" type="submit" disabled={busy || !locationId}>{busy ? "Recording…" : mode === "receipt" ? "Confirm stock received" : "Confirm adjustment"}<span>→</span></button></form></Drawer>;
}

function LocationDrawer({ onClose, onComplete }: { onClose: () => void; onComplete: (message: string) => Promise<void> }) {
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [locationType, setLocationType] = useState("WAREHOUSE");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setBusy(true); setError(null); try { await createInventoryLocation({ code, name, locationType }); await onComplete("Location added"); } catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Unable to add location"); } finally { setBusy(false); } }
  return <Drawer title="Add location" kicker="Inventory / Locations" onClose={onClose}><form className="inventory-form" onSubmit={submit}><Field label="Location code"><input value={code} onChange={(event) => setCode(event.target.value.toUpperCase())} placeholder="MEL-WH-01" required /></Field><Field label="Location name"><input value={name} onChange={(event) => setName(event.target.value)} placeholder="Melbourne warehouse" required /></Field><Field label="Location type"><select value={locationType} onChange={(event) => setLocationType(event.target.value)}><option>WAREHOUSE</option><option>STORE</option><option>SERVICE</option></select></Field>{error && <p className="inventory-form-error" role="alert">{error}</p>}<button className="inventory-submit" type="submit" disabled={busy}>{busy ? "Saving…" : "Add location"}<span>→</span></button></form></Drawer>;
}

function Drawer({ title: drawerTitle, kicker, onClose, children }: { title: string; kicker: string; onClose: () => void; children: React.ReactNode }) { return <div className="admin-drawer-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><aside className="admin-drawer inventory-drawer"><button className="admin-drawer-close" type="button" onClick={onClose}>×</button><span>{kicker}</span><h2>{drawerTitle}</h2>{children}</aside></div>; }
function Field({ label: fieldLabel, optional, children }: { label: string; optional?: boolean; children: React.ReactNode }) { return <label className="inventory-field"><span>{fieldLabel}{optional && <small>Optional</small>}</span>{children}</label>; }
function Status({ value }: { value: string }) { return <span className={`admin-status ${value.toLowerCase().replaceAll("_", "-")}`}>{statusLabel(value)}</span>; }
function statusLabel(value: string) { return ({ ACTIVE: "Active", DRAFT: "Draft", DISCONTINUED: "Discontinued", RECEIPT: "Stock received", ADJUSTMENT: "Stock adjusted", RESERVATION: "Reserved", RELEASE: "Released", FULFILMENT: "Fulfilled", RETURN: "Returned" } as Record<string, string>)[value] ?? label(value); }
function inventoryViewLabel(value: InventoryView) { return ({ stock: "Stock", catalogue: "Component catalogue", movements: "Stock history", locations: "Locations", systems: "Whole systems" })[value]; }
function label(value: string) { return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase().replaceAll("_", " "); }
function signed(value: number) { return value > 0 ? `+${value}` : String(value); }
function money(cents: number) { return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cents / 100); }
function dateTime(value: string) { return new Intl.DateTimeFormat("en-AU", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" }).format(new Date(value)); }
function isMoneyInput(value: string) { return /^[0-9]*(?:\.[0-9]{0,2})?$/.test(value); }
function dollarsToCents(value: string) { const [dollars = "0", cents = ""] = value.split("."); return Number(dollars || "0") * 100 + Number(cents.padEnd(2, "0")); }
function centsToDollars(value: number) { return (value / 100).toFixed(2); }
function formatMoneyInput(value: string) { return centsToDollars(dollarsToCents(value)); }

export default AdminInventoryWorkspace;
