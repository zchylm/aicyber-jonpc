import type { AuthUser } from "../api/auth";
import CompanyFooter from "./CompanyFooter";
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

      <CompanyFooter />
    </>
  );
}

export default SiteSupport;
