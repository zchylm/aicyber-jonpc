import { useState } from "react";
import BrandLockup from "../components/BrandLockup";
import BuildConfigurator from "../components/BuildConfigurator";
import AiAssistant from "../components/AiAssistant";
import HeroVisual from "../components/HeroVisual";
import SystemsSection from "../components/SystemsSection";
import AiInfrastructureSection from "../components/AiInfrastructureSection";
import AuthPanel from "../components/AuthPanel";
import QueueRewardsSection from "../components/QueueRewardsSection";
import PaymentCheckout from "../components/PaymentCheckout";
import SiteSupport from "../components/SiteSupport";
import ShoppingBag from "../components/ShoppingBag";
import type { AuthUser } from "../api/auth";
import { internalPaymentDemoEnabled } from "../config/runtime";
import { navigationItems } from "../data/site";
import "./JonPcHeroPage.css";

function JonPcHeroPage() {
  const [user, setUser] = useState<AuthUser | null>(null);

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

      <section className="hero" aria-labelledby="hero-title">
        <HeroVisual />

      </section>

      <QueueRewardsSection user={user} />

      {internalPaymentDemoEnabled && <PaymentCheckout user={user} onUserChange={setUser} />}

      <SystemsSection />

      <AiInfrastructureSection />

      <BuildConfigurator user={user} />
      <SiteSupport user={user} />
      <AiAssistant />
    </main>
  );
}

export default JonPcHeroPage;
