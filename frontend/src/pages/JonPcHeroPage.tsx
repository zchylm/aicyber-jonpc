import BrandLockup from "../components/BrandLockup";
import BuildConfigurator from "../components/BuildConfigurator";
import AiAssistant from "../components/AiAssistant";
import LogicStrip from "../components/LogicStrip";
import HeroVisual from "../components/HeroVisual";
import SystemsSection from "../components/SystemsSection";
import AuthPanel from "../components/AuthPanel";
import { navigationItems } from "../data/site";
import "./JonPcHeroPage.css";

function JonPcHeroPage() {
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
          {navigationItems.map((item) => <a href={item.href} key={item.label}>{item.label}</a>)}
        </nav>

        <AuthPanel />

        <a className="nav-cta" href="#build">
          <span>Start new build</span>
          <span className="nav-cta-icon" aria-hidden="true">↗</span>
        </a>
      </header>

      <section className="hero" id="top" aria-labelledby="hero-title">
        <HeroVisual />

      </section>

      <LogicStrip />

      <SystemsSection />

      <BuildConfigurator />
      <span id="support" className="anchor-target" />
      <AiAssistant />
    </main>
  );
}

export default JonPcHeroPage;
