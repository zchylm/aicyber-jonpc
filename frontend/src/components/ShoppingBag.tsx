import { useCallback, useEffect, useRef, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey } from "../api/auth";
import { prepareCartCheckout, type PreviewSystem } from "../api/systems";
import { checkoutEnabled } from "../config/runtime";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import "./ShoppingBag.css";

type BagItem = Pick<PreviewSystem, "id" | "sku" | "name" | "priceCents" | "available" | "saleEnabled"> & { quantity: number };

const storageKey = "jonpc:shopping-bag:v1";
const checkoutLinesKey = (reference: string) => `jonpc:shopping-bag-checkout:${reference}`;

function readBag(): BagItem[] {
  try {
    const parsed = JSON.parse(window.localStorage.getItem(storageKey) ?? "[]") as BagItem[];
    return Array.isArray(parsed) ? parsed.filter((item) => item?.id && item.quantity > 0) : [];
  } catch {
    return [];
  }
}

function ShoppingBag({ user }: { user: AuthUser | null }) {
  const [items, setItems] = useState<BagItem[]>(readBag);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(() => new Set());
  const [isOpen, setIsOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const resumeAfterLogin = useRef<{ items: BagItem[]; removeIds: string[] } | null>(null);
  const { open, close, dismiss } = useHistoryPanel("shopping-bag", isOpen, setIsOpen);

  useEffect(() => {
    window.localStorage.setItem(storageKey, JSON.stringify(items));
  }, [items]);

  useEffect(() => {
    const add = (event: Event) => {
      const system = (event as CustomEvent<PreviewSystem>).detail;
      if (!system?.id || !system.saleEnabled || !system.available) return;
      setItems((current) => {
        const existing = current.find((item) => item.id === system.id);
        if (existing) return current.map((item) => item.id === system.id ? { ...item, quantity: Math.min(10, item.quantity + 1), priceCents: system.priceCents, available: system.available } : item);
        return [...current, { id: system.id, sku: system.sku, name: system.name, priceCents: system.priceCents, available: system.available, saleEnabled: system.saleEnabled, quantity: 1 }];
      });
      setSelectedIds(new Set([system.id]));
      setError(null);
      open();
    };
    window.addEventListener("jonpc:add-to-bag", add);
    return () => window.removeEventListener("jonpc:add-to-bag", add);
  }, [open]);

  const prepareCheckout = useCallback(async (checkoutItems: BagItem[], removeIds: string[]) => {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      resumeAfterLogin.current = { items: checkoutItems, removeIds };
      window.dispatchEvent(new CustomEvent("jonpc:open-account"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const prepared = await prepareCartCheckout(token, checkoutItems.map((item) => ({ systemId: item.id, quantity: item.quantity })));
      window.localStorage.setItem(checkoutLinesKey(prepared.requestReference), JSON.stringify(removeIds));
      dismiss();
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
      window.dispatchEvent(new CustomEvent("jonpc:open-checkout", { detail: { requestReference: prepared.requestReference } }));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to prepare checkout.");
    } finally {
      setBusy(false);
    }
  }, [dismiss]);

  const requestCheckout = useCallback((checkoutItems: BagItem[], removeIds: string[]) => {
    if (!checkoutItems.length) return;
    if (!user) {
      resumeAfterLogin.current = { items: checkoutItems, removeIds };
      window.dispatchEvent(new CustomEvent("jonpc:open-account"));
      return;
    }
    void prepareCheckout(checkoutItems, removeIds);
  }, [prepareCheckout, user]);

  useEffect(() => {
    const buyNow = (event: Event) => {
      const system = (event as CustomEvent<PreviewSystem>).detail;
      if (!system?.id || !system.saleEnabled || !system.available) return;
      requestCheckout([{ id: system.id, sku: system.sku, name: system.name, priceCents: system.priceCents, available: system.available, saleEnabled: system.saleEnabled, quantity: 1 }], []);
    };
    window.addEventListener("jonpc:buy-now", buyNow);
    return () => window.removeEventListener("jonpc:buy-now", buyNow);
  }, [requestCheckout]);

  useEffect(() => {
    const removePurchasedLines = (event: Event) => {
      const reference = (event as CustomEvent<{ requestReference?: string }>).detail?.requestReference;
      if (!reference) return;
      const key = checkoutLinesKey(reference);
      try {
        const ids = JSON.parse(window.localStorage.getItem(key) ?? "[]") as string[];
        if (Array.isArray(ids) && ids.length) {
          const removed = new Set(ids);
          setItems((current) => current.filter((item) => !removed.has(item.id)));
          setSelectedIds((current) => new Set([...current].filter((id) => !removed.has(id))));
        }
      } finally {
        window.localStorage.removeItem(key);
      }
    };
    window.addEventListener("jonpc:checkout-committed", removePurchasedLines);
    return () => window.removeEventListener("jonpc:checkout-committed", removePurchasedLines);
  }, []);

  useEffect(() => {
    if (!resumeAfterLogin.current || !user) return;
    const pending = resumeAfterLogin.current;
    resumeAfterLogin.current = null;
    void prepareCheckout(pending.items, pending.removeIds);
  }, [prepareCheckout, user]);

  const selectedItems = items.filter((item) => selectedIds.has(item.id));
  const selectedQuantity = selectedItems.reduce((sum, item) => sum + item.quantity, 0);
  const selectedTotal = selectedItems.reduce((sum, item) => sum + item.priceCents * item.quantity, 0);

  function toggleSelection(id: string) {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  function removeItem(id: string) {
    setItems((current) => current.filter((item) => item.id !== id));
    setSelectedIds((current) => new Set([...current].filter((selectedId) => selectedId !== id)));
  }

  function decreaseQuantity(item: BagItem) {
    if (item.quantity === 1) {
      removeItem(item.id);
      return;
    }
    setItems((current) => current.map((entry) => entry.id === item.id ? { ...entry, quantity: entry.quantity - 1 } : entry));
  }

  return <>
    <button className="shopping-bag-button" type="button" onClick={open} aria-label="Open shopping bag" title="Shopping bag">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6.5 8.5h11l1 12h-13l1-12Z" /><path d="M9 9V6.5a3 3 0 0 1 6 0V9" /></svg>
    </button>
    {isOpen && <div className="shopping-bag-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && close()}>
      <section className="shopping-bag-drawer" role="dialog" aria-modal="true" aria-labelledby="shopping-bag-title">
        <button className="shopping-bag-close" type="button" onClick={close} aria-label="Close shopping bag">×</button>
        <span className="section-kicker">JON. PC / Shopping bag</span>
        <h2 id="shopping-bag-title">Choose what goes next.</h2>
        {!items.length && <div className="shopping-bag-empty"><p>Your shopping bag is empty.</p><button type="button" onClick={() => { dismiss(); document.querySelector("#systems")?.scrollIntoView({ behavior: "smooth" }); }}>Explore systems <span>↓</span></button></div>}
        {items.length > 0 && <>
          <div className="shopping-bag-selection"><span>Select systems to checkout</span>{items.length > 1 && <button type="button" onClick={() => setSelectedIds(selectedItems.length === items.length ? new Set() : new Set(items.map((item) => item.id)))}>{selectedItems.length === items.length ? "Clear" : "Select all"}</button>}</div>
          <div className="shopping-bag-items">{items.map((item) => <article key={item.id}>
            <label className="shopping-bag-select"><input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelection(item.id)} aria-label={`Select ${item.name} for checkout`} /><span aria-hidden="true" /></label>
            <div className="shopping-bag-item-copy"><span>{item.sku}</span><strong>{item.name.replace("JON PC ", "")}</strong><b>{money(item.priceCents * item.quantity)} · incl. GST</b></div>
            <div className="shopping-bag-quantity"><button type="button" aria-label={`Decrease ${item.name} quantity`} onClick={() => decreaseQuantity(item)}>−</button><span>{item.quantity}</span><button type="button" aria-label={`Increase ${item.name} quantity`} onClick={() => setItems((current) => current.map((entry) => entry.id === item.id ? { ...entry, quantity: Math.min(10, entry.quantity + 1) } : entry))}>+</button></div>
            <button className="shopping-bag-remove" type="button" onClick={() => removeItem(item.id)}>Remove</button>
          </article>)}</div>
          <div className="shopping-bag-total"><span>Selected total · incl. GST</span><strong>{selectedQuantity ? money(selectedTotal) : "—"}</strong></div>
          {error && <p className="shopping-bag-error" role="alert">{error}</p>}
          <button className="shopping-bag-checkout" type="button" onClick={() => requestCheckout(selectedItems, selectedItems.map((item) => item.id))} disabled={busy || !checkoutEnabled || !selectedQuantity}>{busy ? "Preparing…" : !checkoutEnabled ? "Checkout unavailable" : selectedQuantity ? `Checkout ${selectedQuantity} ${selectedQuantity === 1 ? "item" : "items"} · ${money(selectedTotal)}` : "Select items to checkout"}<span>↗</span></button>
        </>}
      </section>
    </div>}
  </>;
}

function money(cents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cents / 100);
}

export default ShoppingBag;
