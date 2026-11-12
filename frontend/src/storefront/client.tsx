import { useCallback, useEffect, useRef, useState } from "react";
import { createRoot, hydrateRoot } from "react-dom/client";
import { createPortal } from "react-dom";
import type { AuthUser } from "../api/auth";
import { fetchSystemPreviews } from "../api/systems";
import { fetchRewardSummary, type RewardPublicSummary } from "../api/rewards";
import type { SavedBuild } from "../api/configurator";
import AuthPanel from "../components/AuthPanel";
import ShoppingBag from "../components/ShoppingBag";
import PaymentCheckout from "../components/PaymentCheckout";
import PolicyPanel from "../components/PolicyPanel";
import AiAssistant from "../components/AiAssistant";
import { checkoutEnabled } from "../config/runtime";
import { productPages } from "../data/productPages";
import StorefrontView from "./StorefrontView";
import { pageProduct, pageStructuredData, type StorefrontSnapshot } from "./pages";
import "../index.css";
import "../pages/JonPcHeroPage.css";
import "./storefront.css";

export function PublicPage({ page, systems: initialSystems }: StorefrontSnapshot) {
  const [systems, setSystems] = useState(initialSystems);
  const [verified, setVerified] = useState(false);
  const [error, setError] = useState(false);
  const [founderSummary, setFounderSummary] = useState<RewardPublicSummary | null>(null);
  const requestVersion = useRef(0);
  const refresh = useCallback(() => {
    const version = ++requestVersion.current;
    if (page.kind === "product") {
      void fetchRewardSummary().then(summary => {
        if (version === requestVersion.current) setFounderSummary(summary);
      }).catch(() => { if (version === requestVersion.current) setFounderSummary(null); });
    }
    return fetchSystemPreviews().then(current => {
      if (version !== requestVersion.current) return;
      const known = current.filter(system => productPages[system.sku]);
      const initialProduct = pageProduct(page, initialSystems);
      if (initialProduct && !known.some(system => system.sku === initialProduct.sku)) {
        known.push({ ...initialProduct, available: false, saleEnabled: false });
      }
      setSystems(known);
      setVerified(true);
      setError(false);
      const schema = document.getElementById("storefront-schema");
      if (schema) schema.textContent = JSON.stringify(pageStructuredData(page, known));
    }).catch(() => { if (version === requestVersion.current) { setVerified(false); setError(true); } });
  }, [initialSystems, page]);

  useEffect(() => {
    const update = () => { void refresh(); };
    update();
    const interval = window.setInterval(update, 60_000);
    window.addEventListener("focus", update);
    window.addEventListener("jonpc:payment-completed", update);
    window.addEventListener("jonpc:system-stock-changed", update);
    return () => {
      requestVersion.current += 1;
      window.clearInterval(interval);
      window.removeEventListener("focus", update);
      window.removeEventListener("jonpc:payment-completed", update);
      window.removeEventListener("jonpc:system-stock-changed", update);
    };
  }, [refresh]);

  return <StorefrontView page={page} systems={systems} verified={verified} error={error} founderSummary={founderSummary} onRetry={() => { void refresh(); }} />;
}

export function StorefrontCommerce() {
  const [user, setUser] = useState<AuthUser | null>(null);
  useEffect(() => {
    const resumeBuild = (event: Event) => {
      const build = (event as CustomEvent<SavedBuild>).detail;
      if (build?.id) window.location.assign(`/?resumeBuild=${encodeURIComponent(build.id)}#build`);
    };
    const viewSavings = () => window.location.assign("/?showSavings=1#queue-rewards");
    window.addEventListener("jonpc:load-build", resumeBuild);
    window.addEventListener("jonpc:open-rewards", viewSavings);
    return () => {
      window.removeEventListener("jonpc:load-build", resumeBuild);
      window.removeEventListener("jonpc:open-rewards", viewSavings);
    };
  }, []);
  return <>
    {createPortal(<><ShoppingBag user={user} /><AuthPanel onAuthChange={setUser} /></>, document.getElementById("storefront-account")!)}
    {checkoutEnabled && <PaymentCheckout user={user} onUserChange={setUser} />}
    <PolicyPanel />
    <AiAssistant />
  </>;
}

const snapshotElement = document.getElementById("storefront-data");
if (snapshotElement) {
  const snapshot = JSON.parse(snapshotElement.textContent!) as StorefrontSnapshot;
  hydrateRoot(document.getElementById("storefront-root")!, <PublicPage {...snapshot} />);
  createRoot(document.getElementById("storefront-runtime")!).render(<StorefrontCommerce />);
}
