import { useEffect, useRef, useState } from "react";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import type { PolicyId } from "./policyEvents";
import "./PolicyPanel.css";

type Policy = {
  label: string;
  title: string;
  intro: string;
  sections: { title: string; body: string[] }[];
};

const policies: Record<PolicyId, Policy> = {
  terms: {
    label: "Terms & Conditions",
    title: "Clear terms, built around your order.",
    intro: "These terms apply to purchases from AI CYBER AUSTRALIA PTY LTD trading as JON. PC.",
    sections: [
      { title: "Orders", body: ["Prices are in Australian dollars and include GST unless stated otherwise.", "An order is accepted after cleared payment is confirmed. If stock becomes unavailable, a genuine pricing error occurs or an order cannot lawfully be fulfilled, we may cancel and promptly refund it."] },
      { title: "Specifications", body: ["Product images may be illustrative. A supplied system will match the confirmed specification or an equivalent you expressly approve. We will ask before making a material substitution."] },
      { title: "Your rights", body: ["Nothing in these terms excludes or limits rights and remedies that cannot be excluded under the Australian Consumer Law."] },
    ],
  },
  shipping: {
    label: "Shipping & Pickup",
    title: "Melbourne delivery, clearly timed.",
    intro: "Complete systems are prepared and dispatched after cleared payment.",
    sections: [
      { title: "Delivery", body: ["Melbourne metro delivery is A$25 including GST for eligible postcodes. We build, test and dispatch within 7–14 business days unless your accepted quote states otherwise.", "Carrier transit begins after dispatch and is not included in the 7–14 business-day commitment. Tracking is provided when available."] },
      { title: "Pickup", body: ["West Melbourne pickup from 205 Kensington Road is free and by appointment after we confirm your system is ready."] },
      { title: "Delivery check", body: ["After recorded delivery, you can confirm receipt or report a problem in My Orders. If no issue is reported, receipt is operationally confirmed after 48 hours. This does not limit your Australian Consumer Law rights."] },
    ],
  },
  returns: {
    label: "Returns & Refunds",
    title: "Problems are handled under Australian law.",
    intro: "Contact support with your order reference so we can assess the right remedy quickly.",
    sections: [
      { title: "Faults", body: ["If a product does not meet an Australian Consumer Guarantee, the available remedy may be a repair, replacement or refund depending on the nature of the failure.", "For a major failure, you may be entitled to choose a refund or replacement. For a non-major failure, we may first repair the product within a reasonable time."] },
      { title: "Proof and return costs", body: ["We may ask for reasonable proof of purchase, such as your JON. PC invoice, order record or payment statement; original packaging is not required for a faulty-product claim.", "If an assessed product is confirmed faulty, we reimburse reasonable return costs already paid. Where a faulty product is large or difficult to return, we will arrange collection or cover reasonable transport as required by law."] },
      { title: "Refunds", body: ["Approved refunds are initiated promptly to the original payment method where reasonably possible. Bank and card processing times are controlled by the relevant financial institution.", "Change-of-mind returns are separate from Australian Consumer Law remedies and will apply only if a specific voluntary policy is displayed for the product."] },
    ],
  },
  warranty: {
    label: "Warranty & Repairs",
    title: "Three-year parts. Two-year return-to-base.",
    intro: "Complete JON. PC systems include a 3-year parts / 2-year return-to-base warranty in addition to your Australian Consumer Law rights.",
    sections: [
      { title: "Covered", body: ["The warranty covers defects in supplied components, materials and workmanship under normal intended use."] },
      { title: "Not covered", body: ["Accidental or liquid damage, misuse, unauthorised modification, malware, data loss, cosmetic wear and damage caused by external equipment are not covered unless the law requires otherwise.", "Installing an ordinary user-upgradeable component does not automatically void the whole warranty; coverage may be declined only where the change caused or contributed to the fault."] },
      { title: "Before repair", body: ["Back up and remove sensitive data where possible. Before a repair that may expose stored user data or use refurbished parts, we will provide any notice required by Australian Consumer Law.", "The JON. PC 3-year parts / 2-year return-to-base warranty is an additional promise. Australian Consumer Law rights may continue beyond those periods depending on the product and circumstances."] },
    ],
  },
  payment: {
    label: "Payment Information",
    title: "Two ways to pay, one verified order.",
    intro: "Your original price, any Founder discount, delivery and final total are shown before payment.",
    sections: [
      { title: "Secure online payment", body: ["Stripe processes supported online payments. JON. PC does not store full card numbers. An available Founder place is held while the secure checkout remains open."] },
      { title: "Bank transfer", body: ["Use the unique reference shown at checkout. The order and any Founder discount are reserved for 24 hours, but clicking ‘Submit for review’ is not proof of payment.", "Payment is confirmed only after our team verifies cleared funds in the company bank account."] },
      { title: "Safety", body: ["We will never ask for an online-banking password, card PIN or one-time security code."] },
    ],
  },
  custom: {
    label: "Custom PC Build Terms",
    title: "Configure first. Approve the final quote.",
    intro: "Your online selections and budget guide our review; they are not a final price or offer.",
    sections: [
      { title: "Review", body: ["We check compatibility, live component availability and supplier pricing before issuing a final written quote."] },
      { title: "Approval", body: ["No payment is taken until you accept a quote showing the final specification, GST-inclusive total, delivery or pickup charge and dispatch commitment.", "A material substitution or price change requires your approval."] },
    ],
  },
  cashback: {
    label: "Founder 5% Discount",
    title: "5% off eligible product value now.",
    intro: "Fifty eligible order places are available each calendar month, measured in Melbourne time.",
    sections: [
      { title: "Eligibility", body: ["Each eligible order uses one monthly place. There is no per-customer limit, but every order consumes a separate place. Unused places do not roll over.", "The discount is 5% of the product subtotal. Delivery charges are excluded."] },
      { title: "Reservation", body: ["A place is held while Stripe checkout remains open or for the stated bank-transfer window. Failed, cancelled or expired payments release the place.", "The original product price, Founder discount, delivery and final amount are shown before payment."] },
      { title: "Cancellations and refunds", body: ["A cancelled unpaid order releases its place. Eligible same-month cancellations and full refunds before fulfilment may return the place to that month.", "Refunds are based on the amount actually paid after discount. Places are not carried into a later month."] },
    ],
  },
  privacy: {
    label: "Privacy",
    title: "Only the information needed to serve you.",
    intro: "This overview explains how JON. PC handles personal information used for accounts, orders and support.",
    sections: [
      { title: "What we collect", body: ["We collect account and contact details, delivery information, order and payment status, support messages, and security records needed to operate the service. We do not store full card numbers."] },
      { title: "How we use it", body: ["We use this information to provide quotes, process orders, arrange delivery, prevent misuse, provide support and meet legal obligations. Service providers process information only where needed to deliver these functions."] },
      { title: "Service providers", body: ["Information may be shared only as needed with payment processors, email and hosting providers, delivery carriers, professional advisers and authorities where required by law. JON. PC does not sell personal information."] },
      { title: "Your choices", body: ["Email support@jonpc.com.au to request access or correction, or to raise a privacy concern. We will verify identity before disclosing account information and explain how the concern will be handled."] },
    ],
  },
};

function PolicyPanel() {
  const [active, setActive] = useState<PolicyId>("terms");
  const [isOpen, setIsOpen] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const contentRef = useRef<HTMLElement>(null);
  const { open, close } = useHistoryPanel("policies", isOpen, setIsOpen);

  useEffect(() => {
    const show = (event: Event) => {
      const policy = (event as CustomEvent<{ policy?: PolicyId }>).detail?.policy;
      if (policy && policies[policy]) setActive(policy);
      open();
    };
    window.addEventListener("jonpc:open-policy", show);
    return () => window.removeEventListener("jonpc:open-policy", show);
  }, [open]);

  useEffect(() => {
    if (!isOpen) return;
    const escape = (event: KeyboardEvent) => event.key === "Escape" && close();
    document.addEventListener("keydown", escape);
    return () => document.removeEventListener("keydown", escape);
  }, [close, isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const content = contentRef.current;
    if (!content) return;
    content.scrollTop = 0;
    const update = () => setHasMore(content.scrollTop + content.clientHeight < content.scrollHeight - 8);
    const frame = window.requestAnimationFrame(update);
    content.addEventListener("scroll", update);
    window.addEventListener("resize", update);
    return () => {
      window.cancelAnimationFrame(frame);
      content.removeEventListener("scroll", update);
      window.removeEventListener("resize", update);
    };
  }, [active, isOpen]);

  if (!isOpen) return null;
  const policy = policies[active];
  return <div className="policy-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && close()}>
    <section className="policy-panel" role="dialog" aria-modal="true" aria-labelledby="policy-title">
      <button className="policy-close" type="button" onClick={close} aria-label="Close policies">×</button>
      <aside className="policy-index" aria-label="Policies">
        <span>JON. PC / Information</span>
        {Object.entries(policies).map(([id, item]) => <button className={active === id ? "active" : ""} type="button" key={id} onClick={() => setActive(id as PolicyId)}>{item.label}</button>)}
      </aside>
      <article className="policy-content" ref={contentRef}>
        <span className="section-kicker">{policy.label}</span>
        <h2 id="policy-title">{policy.title}</h2>
        <p className="policy-intro">{policy.intro}</p>
        <div className="policy-sections">{policy.sections.map((section) => <section key={section.title}>
          <h3>{section.title}</h3>
          {section.body.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
        </section>)}</div>
        <footer className="policy-contact"><span>End of {policy.label}</span><p>Questions? <a href="mailto:support@jonpc.com.au">support@jonpc.com.au</a></p></footer>
      </article>
      {hasMore && <button className="policy-scroll-cue" type="button" onClick={() => contentRef.current?.scrollBy({ top: Math.max(320, contentRef.current.clientHeight * .65), behavior: "smooth" })}>Continue reading <span aria-hidden="true">↓</span></button>}
    </section>
  </div>;
}

export default PolicyPanel;
