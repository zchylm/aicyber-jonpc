import { openPolicy } from "./policyEvents";
import "./PurchaseAssurance.css";

export default function PurchaseAssurance() {
  return <section className="purchase-assurance-section" aria-label="The JON.PC promise">
    <nav className="purchase-assurance" aria-label="Delivery and warranty">
    <button className="purchase-assurance-warranty" type="button" onClick={() => openPolicy("warranty")}>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z" /><path d="m8 12 3 3 5-6" /></svg>
      <span><strong>3-year warranty</strong><small>Parts, labour & return shipping</small></span>
    </button>
    <button type="button" onClick={() => openPolicy("shipping")}>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="M4 15a8 8 0 1 1 16 0v3H4v-3Z" /><path d="m12 14 4-4M6 15h2m8 0h2M8 8l1 2m6-2-1 2" /><circle cx="12" cy="14" r="1" /></svg>
      <span><strong>Stress tested</strong><small>Stability & thermal checks</small></span>
    </button>
    <button type="button" onClick={() => openPolicy("shipping")}>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" /><circle cx="12" cy="10" r="2.5" /></svg>
      <span><strong>Local pickup</strong><small>West Melbourne</small></span>
    </button>
    <button type="button" onClick={() => openPolicy("shipping")}>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="m3 7 9-4 9 4v10l-9 4-9-4V7Zm0 0 9 4 9-4M12 11v10M7.5 5l9 4" /></svg>
      <span><strong>7–14 business days</strong><small>Dispatch after cleared payment</small></span>
    </button>
    </nav>
  </section>;
}
