import BrandLockup from "./BrandLockup";
import { openPolicy } from "./policyEvents";
import "./SiteSupport.css";

function CompanyFooter() {
  return (
    <footer className="company-footer">
      <div className="company-footer-main">
        <div className="company-footer-brand">
          <BrandLockup />
          <p>Purpose-built computers, clearly configured.</p>
          <div className="company-footer-company">
            <strong>AI CYBER AUSTRALIA PTY LTD</strong>
            <small>ABN 22 689 546 450</small>
          </div>
        </div>
        <div className="company-footer-block company-footer-links">
          <span>Customer support</span>
          <button type="button" onClick={() => openPolicy("shipping")}>Shipping & pickup</button>
          <button type="button" onClick={() => openPolicy("returns")}>Returns & refunds</button>
          <button type="button" onClick={() => openPolicy("warranty")}>Warranty & repairs</button>
        </div>
        <div className="company-footer-block company-footer-links">
          <span>Legal & policies</span>
          {(["terms", "privacy", "payment", "custom", "cashback"] as const).map((policy) =>
            <button type="button" key={policy} onClick={() => openPolicy(policy)}>{({ terms: "Terms & conditions", privacy: "Privacy", payment: "Payment information", custom: "Custom build terms", cashback: "Founder 5% discount" })[policy]}</button>)}
        </div>
        <address className="company-footer-block">
          <span>Contact</span>
          <a href="mailto:support@jonpc.com.au">support@jonpc.com.au</a>
          <small>205 Kensington Rd, West Melbourne VIC 3003</small>
        </address>
      </div>
      <div className="company-footer-base">
        <span>© {new Date().getFullYear()} AI CYBER AUSTRALIA PTY LTD</span>
        <a href="#top">Back to top ↑</a>
      </div>
    </footer>
  );
}

export default CompanyFooter;
