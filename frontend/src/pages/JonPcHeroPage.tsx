import { useState } from "react";
import BrandLockup from "../components/BrandLockup";
import BuildConfigurator from "../components/BuildConfigurator";
import AiAssistant from "../components/AiAssistant";
import LogicStrip from "../components/LogicStrip";
import HeroVisual from "../components/HeroVisual";
import SystemsSection from "../components/SystemsSection";
import AuthPanel from "../components/AuthPanel";
import QueueRewardsSection from "../components/QueueRewardsSection";
import PaymentCheckout from "../components/PaymentCheckout";
import type { AuthUser } from "../api/auth";
import { navigationItems } from "../data/site";
import "./JonPcHeroPage.css";

function JonPcHeroPage() {
  const [user, setUser] = useState<AuthUser | null>(null);

  return (
    <main className="site-shell">
      <div className="hero-grid" aria-hidden="true" />
      <div className="hero-glow hero-glow-left" aria-hidden="true" />
      <div className="hero-glow hero-glow-bottom" aria-hidden="true" />

      <header className="site-nav">
        <a className="brand" href="#top" aria-label="JON. PC home">
          <BrandLockup />
        </a>

        <nav className="nav-links" aria-label="Primary navigation">
          {navigationItems.map((item) => {
            const isRewards = item.href === "#queue-rewards";
            return (
              <a
                href={item.href}
                key={item.label}
                onClick={(event) => {
                  if (item.href === "#build") window.dispatchEvent(new CustomEvent("jonpc:start-new-build"));
                  if (isRewards && user) {
                    event.preventDefault();
                    window.dispatchEvent(new CustomEvent("jonpc:open-rewards"));
                  }
                }}
              >
                {isRewards && user ? "My rewards" : item.label}
              </a>
            );
          })}
        </nav>

        <AuthPanel onAuthChange={setUser} />

        <a className="nav-cta" href="#build" onClick={() => window.dispatchEvent(new CustomEvent("jonpc:start-new-build"))}>
          <span>Start new build</span>
          <span className="nav-cta-icon" aria-hidden="true">↗</span>
        </a>
      </header>

      <section className="hero" id="top" aria-labelledby="hero-title">
        <HeroVisual />

      </section>

      <QueueRewardsSection user={user} />

      {import.meta.env.DEV && <PaymentCheckout user={user} />}

      <LogicStrip />

      <SystemsSection />

      <BuildConfigurator user={user} />
      <span id="support" className="anchor-target" />
      <AiAssistant />
    </main>
  );
}

export default JonPcHeroPage;
