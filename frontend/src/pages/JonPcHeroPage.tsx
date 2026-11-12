import { useEffect, useLayoutEffect, useState } from "react";
import BrandLockup from "../components/BrandLockup";
import BuildConfigurator from "../components/BuildConfigurator";
import AiAssistant from "../components/AiAssistant";
import HeroVisual from "../components/HeroVisual";
import SystemsSection from "../components/SystemsSection";
import AiInfrastructureSection from "../components/AiInfrastructureSection";
import AuthPanel from "../components/AuthPanel";
import QueueRewardsSection from "../components/QueueRewardsSection";
import PaymentCheckout from "../components/PaymentCheckout";
import PolicyPanel from "../components/PolicyPanel";
import SiteSupport from "../components/SiteSupport";
import ShoppingBag from "../components/ShoppingBag";
import PurchaseAssurance from "../components/PurchaseAssurance";
import { authTokenKey, type AuthUser } from "../api/auth";
import { fetchSavedBuilds } from "../api/configurator";
import { checkoutEnabled } from "../config/runtime";
import { navigationItems } from "../data/site";
import "./JonPcHeroPage.css";

function JonPcHeroPage() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [resumeError, setResumeError] = useState(false);

  useLayoutEffect(() => {
    const navigation = performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming | undefined;
    if (navigation?.type !== "reload") return;
    const url = new URL(window.location.href);
    const hasAction = url.searchParams.get("open") === "orders"
      || ["resumeBuild", "showSavings", "stripe_result", "stripe_payment", "verifyEmailToken", "resetPasswordToken"]
        .some(key => url.searchParams.has(key));
    if (hasAction) return;
    // A normal homepage refresh starts at Hero; incoming links and action returns retain their targets.
    url.hash = "";
    window.history.replaceState(window.history.state, "", url.pathname + url.search);
    window.scrollTo({ top: 0, left: 0, behavior: "instant" });
  }, []);

  useEffect(() => {
    let observer: MutationObserver | null = null;
    const navigate = () => {
      observer?.disconnect();
      const hash = window.location.hash;
      if (!navigationItems.some(item => item.href === hash)
          || new URL(window.location.href).searchParams.has("resumeBuild")) return;
      const scrollToModule = () => {
        if (window.location.hash !== hash) return true;
        // Wait for the catalogue so sections below it do not move after scrolling.
        if (document.querySelector('#systems[aria-busy="true"]')) return false;
        const target = document.getElementById(hash.slice(1));
        if (!target) return false;
        target.scrollIntoView({ behavior: "smooth", block: "start" });
        return true;
      };
      if (scrollToModule()) return;
      observer = new MutationObserver(() => { if (scrollToModule()) observer?.disconnect(); });
      observer.observe(document.getElementById("root")!, { childList: true, subtree: true });
    };
    navigate();
    window.addEventListener("hashchange", navigate);
    return () => { observer?.disconnect(); window.removeEventListener("hashchange", navigate); };
  }, []);

  useEffect(() => {
    if (!user) return;
    const url = new URL(window.location.href);
    const buildId = url.searchParams.get("resumeBuild");
    const showSavings = url.searchParams.get("showSavings") === "1";
    if (!buildId && !showSavings) return;
    let active = true;
    const clearReturn = () => {
      url.searchParams.delete("resumeBuild");
      url.searchParams.delete("showSavings");
      window.history.replaceState(window.history.state, "", url.pathname + url.search + url.hash);
    };
    if (buildId) {
      const token = window.localStorage.getItem(authTokenKey);
      if (!token) return;
      void fetchSavedBuilds(token).then(builds => {
        if (!active) return;
        const build = builds.find(item => item.id === buildId);
        if (!build) { setResumeError(true); return; }
        clearReturn();
        window.dispatchEvent(new CustomEvent("jonpc:load-build", { detail: build }));
      }).catch(() => { if (active) setResumeError(true); });
    } else {
      clearReturn();
      window.dispatchEvent(new CustomEvent("jonpc:open-rewards"));
    }
    return () => { active = false; };
  }, [user]);

  return (
    <main className="site-shell" id="top">
      <div className="hero-grid" aria-hidden="true" />
      <div className="hero-glow hero-glow-left" aria-hidden="true" />
      <div className="hero-glow hero-glow-bottom" aria-hidden="true" />

      <header className="site-nav">
        <a className="brand" href="#top" aria-label="JON. PC home">
          <BrandLockup />
        </a>

        <nav className="nav-links" aria-label="Primary navigation">
          {navigationItems.map((item) => <a href={item.href} key={item.label}>{item.label}</a>)}
        </nav>

        <div className="nav-actions">
          <ShoppingBag user={user} />
          <AuthPanel onAuthChange={setUser} />
        </div>
      </header>

      {resumeError && <p className="storefront-resume-error" role="alert">Unable to restore this configuration. <button type="button" onClick={() => window.dispatchEvent(new CustomEvent("jonpc:open-builds"))}>Open saved builds</button></p>}

      <section className="hero" aria-labelledby="hero-title">
        <HeroVisual />
        <nav className="hero-categories" aria-label="Shop by PC type">
          <a href="#gaming-pcs"><span className="hero-category-label">Play</span><strong>Gaming PCs</strong><span className="hero-category-arrow" aria-hidden="true">↗</span></a>
          <a href="#compact-ai-systems"><span className="hero-category-label">Work & AI</span><strong>Mini PCs</strong><span className="hero-category-arrow" aria-hidden="true">↗</span></a>
          <a href="#build"><span className="hero-category-label">Make it yours</span><strong>Custom PCs</strong><span className="hero-category-arrow" aria-hidden="true">↗</span></a>
        </nav>
      </section>

      <PurchaseAssurance />

      <QueueRewardsSection user={user} />

      {checkoutEnabled && <PaymentCheckout user={user} onUserChange={setUser} />}

      <SystemsSection />

      <AiInfrastructureSection />

      <BuildConfigurator user={user} />
      <SiteSupport user={user} />
      <PolicyPanel />
      <AiAssistant />
    </main>
  );
}

export default JonPcHeroPage;
