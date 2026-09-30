import type { AuthUser } from "../api/auth";
import BrandLockup from "./BrandLockup";
import "./SiteSupport.css";

function SiteSupport({ user }: { user: AuthUser | null }) {
  function openOrders() {
    window.dispatchEvent(new CustomEvent(user ? "jonpc:open-orders" : "jonpc:open-account"));
  }

  return (
    <>
      <section className="support-section" id="support" aria-labelledby="support-title">
        <div className="support-intro">
          <span className="support-kicker"><i /> JON. PC / Support</span>
          <h2 id="support-title">Support that knows your PC.</h2>
          <p>From your first question to the system on your desk.</p>
        </div>

        <div className="support-routes" aria-label="Support options">
          <button type="button" onClick={openOrders}>
            <span>01 / Orders</span>
            <strong>Order help</strong>
            <small>Orders, payments and invoices.</small>
            <b aria-hidden="true">→</b>
          </button>
          <button type="button" onClick={() => window.dispatchEvent(new CustomEvent("jonpc:open-ai"))}>
            <span>02 / JON. AI</span>
            <strong>Build advice</strong>
            <small>Ask anything about your next PC.</small>
            <b aria-hidden="true">→</b>
          </button>
          <a href="mailto:support@jonpc.com.au">
            <span>03 / Melbourne</span>
            <strong>Contact our team</strong>
            <small>Talk directly with JON. PC.</small>
            <b aria-hidden="true">↗</b>
          </a>
        </div>
      </section>

      <footer className="company-footer">
        <div className="company-footer-main">
          <div className="company-footer-brand">
            <BrandLockup />
            <p>Purpose-built computers, clearly configured.</p>
          </div>
          <div className="company-footer-block">
            <span>Company</span>
            <strong>AI CYBER AUSTRALIA PTY LTD</strong>
            <small>ABN 22 689 546 450</small>
          </div>
          <address className="company-footer-block">
            <span>Contact</span>
            <a href="mailto:support@jonpc.com.au">support@jonpc.com.au</a>
            <a href="tel:+61436365016">+61 436 365 016</a>
            <small>205 Kensington Rd, West Melbourne VIC 3003</small>
          </address>
        </div>
        <div className="company-footer-base">
          <span>© {new Date().getFullYear()} AI CYBER AUSTRALIA PTY LTD</span>
          <a href="#top">Back to top ↑</a>
        </div>
      </footer>
    </>
  );
}

export default SiteSupport;
