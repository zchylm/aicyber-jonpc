import { useCallback, useEffect, useRef, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey } from "../api/auth";
import { prepareCartCheckout, type PreviewSystem } from "../api/systems";
import { internalPaymentDemoEnabled } from "../config/runtime";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import "./ShoppingBag.css";

type BagItem = Pick<PreviewSystem, "id" | "sku" | "name" | "priceCents" | "available" | "saleEnabled"> & { quantity: number };

const storageKey = "jonpc:shopping-bag:v1";

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
  const [isOpen, setIsOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const resumeAfterLogin = useRef(false);
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
      setError(null);
      open();
    };
    window.addEventListener("jonpc:add-to-bag", add);
    return () => window.removeEventListener("jonpc:add-to-bag", add);
  }, [open]);

  useEffect(() => {
    const clear = () => setItems([]);
    window.addEventListener("jonpc:payment-completed", clear);
    return () => window.removeEventListener("jonpc:payment-completed", clear);
  }, []);

  const checkout = useCallback(async () => {
    if (!items.length) return;
    if (!user) {
      resumeAfterLogin.current = true;
      window.dispatchEvent(new CustomEvent("jonpc:open-account"));
      return;
    }
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      window.dispatchEvent(new CustomEvent("jonpc:open-account"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const prepared = await prepareCartCheckout(token, items.map((item) => ({ systemId: item.id, quantity: item.quantity })));
      dismiss();
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
      window.dispatchEvent(new CustomEvent("jonpc:open-checkout", { detail: { requestReference: prepared.requestReference } }));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to prepare checkout.");
    } finally {
      setBusy(false);
    }
  }, [dismiss, items, user]);

  useEffect(() => {
    if (!resumeAfterLogin.current || !user) return;
    resumeAfterLogin.current = false;
    void checkout();
  }, [checkout, user]);

  const total = items.reduce((sum, item) => sum + item.priceCents * item.quantity, 0);

  return <>
    <button className="shopping-bag-button" type="button" onClick={open} aria-label="Open shopping bag" title="Shopping bag">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6.5 8.5h11l1 12h-13l1-12Z" /><path d="M9 9V6.5a3 3 0 0 1 6 0V9" /></svg>
    </button>
    {isOpen && <div className="shopping-bag-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && close()}>
      <section className="shopping-bag-drawer" role="dialog" aria-modal="true" aria-labelledby="shopping-bag-title">
        <button className="shopping-bag-close" type="button" onClick={close} aria-label="Close shopping bag">×</button>
        <span className="section-kicker">JON. PC / Shopping bag</span>
        <h2 id="shopping-bag-title">Ready when you are.</h2>
        {!items.length && <div className="shopping-bag-empty"><p>Your shopping bag is empty.</p><button type="button" onClick={() => { dismiss(); document.querySelector("#systems")?.scrollIntoView({ behavior: "smooth" }); }}>Explore systems <span>↓</span></button></div>}
        {items.length > 0 && <>
          <div className="shopping-bag-items">{items.map((item) => <article key={item.id}>
            <div><span>{item.sku}</span><strong>{item.name.replace("JON PC ", "")}</strong><small>{money(item.priceCents)} incl. GST</small></div>
            <div className="shopping-bag-quantity"><button type="button" aria-label={`Decrease ${item.name} quantity`} onClick={() => setItems((current) => current.flatMap((entry) => entry.id !== item.id ? [entry] : entry.quantity === 1 ? [] : [{ ...entry, quantity: entry.quantity - 1 }]))}>−</button><span>{item.quantity}</span><button type="button" aria-label={`Increase ${item.name} quantity`} onClick={() => setItems((current) => current.map((entry) => entry.id === item.id ? { ...entry, quantity: Math.min(10, entry.quantity + 1) } : entry))}>+</button></div>
            <button className="shopping-bag-remove" type="button" onClick={() => setItems((current) => current.filter((entry) => entry.id !== item.id))}>Remove</button>
          </article>)}</div>
          <div className="shopping-bag-total"><span>Total incl. GST</span><strong>{money(total)}</strong></div>
          {error && <p className="shopping-bag-error" role="alert">{error}</p>}
          <button className="shopping-bag-checkout" type="button" onClick={() => void checkout()} disabled={busy || !internalPaymentDemoEnabled}>{busy ? "Preparing…" : internalPaymentDemoEnabled ? "Continue to checkout" : "Checkout unavailable"}<span>↗</span></button>
        </>}
      </section>
    </div>}
  </>;
}

function money(cents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cents / 100);
}

export default ShoppingBag;
