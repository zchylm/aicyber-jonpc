import { useEffect, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey, fetchCurrentUser, sendEmailVerification } from "../api/auth";
import { completeMockPayment, createMockPayment, downloadInvoicePdf, fetchDeliveryDetails, saveDeliveryDetails, type DeliveryDetails, type MockPayment } from "../api/payments";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import "./PaymentCheckout.css";

type PaymentCheckoutProps = {
  user: AuthUser | null;
  onUserChange: (user: AuthUser) => void;
};

function PaymentCheckout({ user, onUserChange }: PaymentCheckoutProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [requestReference, setRequestReference] = useState<string | null>(null);
  const [payment, setPayment] = useState<MockPayment | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [verificationBusy, setVerificationBusy] = useState(false);
  const [verificationMessage, setVerificationMessage] = useState<string | null>(null);
  const [delivery, setDelivery] = useState<DeliveryDetails>({ recipientName: "", phone: "", addressLine1: "", addressLine2: "", suburb: "", state: "VIC", postcode: "" });
  const [deliveryLoadedFor, setDeliveryLoadedFor] = useState<string | null>(null);
  const deliveryLoading = Boolean(isOpen && user?.emailVerified && requestReference && deliveryLoadedFor !== requestReference);
  const { open: openCheckoutPanel, close: closeCheckoutPanel, dismiss: dismissCheckoutPanel } = useHistoryPanel("checkout", isOpen, setIsOpen);

  useEffect(() => {
    const openCheckout = (event: Event) => {
      const reference = (event as CustomEvent<{ requestReference?: string }>).detail?.requestReference;
      if (!reference) return;
      if (!user) {
        window.dispatchEvent(new CustomEvent("jonpc:open-account"));
        return;
      }
      openCheckoutPanel();
      setRequestReference(reference);
      setPayment(null);
      setError(null);
      setVerificationMessage(null);
      setDeliveryLoadedFor(null);
      setDelivery({ recipientName: user.displayName ?? "", phone: "", addressLine1: "", addressLine2: "", suburb: "", state: "VIC", postcode: "" });
    };
    window.addEventListener("jonpc:open-checkout", openCheckout);
    return () => window.removeEventListener("jonpc:open-checkout", openCheckout);
  }, [openCheckoutPanel, user]);

  useEffect(() => {
    if (!isOpen || !user?.emailVerified || !requestReference) return;
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    let cancelled = false;
    void fetchDeliveryDetails(token, requestReference).then((saved) => {
      if (!cancelled && saved) setDelivery(saved);
    }).catch((requestError) => {
      if (!cancelled) setError(requestError instanceof Error ? requestError.message : "Unable to load delivery address.");
    }).finally(() => { if (!cancelled) setDeliveryLoadedFor(requestReference); });
    return () => { cancelled = true; };
  }, [isOpen, requestReference, user?.emailVerified]);

  useEffect(() => {
    if (!isOpen || !user || user.emailVerified) return;

    const refreshVerification = async () => {
      const token = window.localStorage.getItem(authTokenKey);
      if (!token) return;
      try {
        const currentUser = await fetchCurrentUser(token);
        if (currentUser.emailVerified) {
          setVerificationMessage(null);
          onUserChange(currentUser);
          window.dispatchEvent(new CustomEvent("jonpc:auth-refresh"));
        }
      } catch {
        // The normal checkout request will surface authentication failures.
      }
    };
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") void refreshVerification();
    };
    const intervalId = window.setInterval(() => void refreshVerification(), 3000);
    window.addEventListener("focus", refreshVerification);
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", refreshVerification);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [isOpen, onUserChange, user]);

  useEffect(() => {
    if (!isOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !busy) closeCheckoutPanel();
    };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [busy, closeCheckoutPanel, isOpen]);

  async function beginCheckout(reference: string, retry = false) {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      setError("Sign in again to continue to checkout.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const storageKey = `jonpc:mock-payment:${reference}`;
      const key = retry ? crypto.randomUUID() : window.sessionStorage.getItem(storageKey) ?? crypto.randomUUID();
      window.sessionStorage.setItem(storageKey, key);
      setPayment(await createMockPayment(token, reference, key));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to prepare checkout.");
    } finally {
      setBusy(false);
    }
  }

  async function confirmDelivery(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const phone = delivery.phone.replace(/[\s()-]/g, "");
    if (!/^(?:0[42378]\d{8}|\+61[42378]\d{8})$/.test(phone)) {
      setError("Enter an Australian mobile or landline number, such as 04xx xxx xxx or +61 4xx xxx xxx.");
      return;
    }
    const token = window.localStorage.getItem(authTokenKey);
    if (!token || !requestReference) return;
    setBusy(true);
    setError(null);
    try {
      await saveDeliveryDetails(token, requestReference, delivery);
      await beginCheckout(requestReference);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to confirm delivery address.");
    } finally {
      setBusy(false);
    }
  }

  async function resendVerification() {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    setVerificationBusy(true);
    setVerificationMessage(null);
    try {
      await sendEmailVerification(token);
      setVerificationMessage("Verification email sent.");
    } catch (requestError) {
      setVerificationMessage(requestError instanceof Error ? requestError.message : "Unable to send verification email.");
    } finally {
      setVerificationBusy(false);
    }
  }

  async function complete(outcome: "SUCCEEDED" | "FAILED") {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token || !payment) return;
    setBusy(true);
    setError(null);
    try {
      const response = await completeMockPayment(token, payment.paymentId, outcome);
      setPayment(response);
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
      if (response.status === "SUCCEEDED") {
        window.dispatchEvent(new CustomEvent("jonpc:payment-completed"));
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to complete payment.");
    } finally {
      setBusy(false);
    }
  }

  function viewRewards() {
    dismissCheckoutPanel();
    window.dispatchEvent(new CustomEvent("jonpc:open-rewards"));
  }

  function payLater() {
    dismissCheckoutPanel();
    window.dispatchEvent(new CustomEvent("jonpc:open-orders"));
  }

  async function downloadInvoice() {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token || !payment?.invoiceId || !payment.invoiceNumber) return;
    setError(null);
    try {
      await downloadInvoicePdf(token, payment.invoiceId, payment.invoiceNumber);
    } catch (downloadError) {
      setError(downloadError instanceof Error ? downloadError.message : "Unable to download invoice.");
    }
  }

  if (!isOpen) return null;

  return (
    <div className="payment-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) closeCheckoutPanel(); }}>
      <section className="payment-modal" role="dialog" aria-modal="true" aria-labelledby="payment-title">
        <button className="payment-close" type="button" onClick={closeCheckoutPanel} disabled={busy} aria-label="Close checkout">×</button>

        {deliveryLoading && !payment && user?.emailVerified && (
          <div className="payment-loading" aria-live="polite"><i /> Preparing your secure checkout...</div>
        )}

        {!user?.emailVerified && (
          <div className="payment-verification" aria-live="polite">
            <span className="section-kicker">JON. PC / Checkout</span>
            <h2 id="payment-title">Verify to continue.</h2>
            <p className="payment-verification-email">{user?.email}</p>
            {verificationMessage && <p className="payment-verification-message" role="status">{verificationMessage}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={resendVerification} disabled={verificationBusy}>
              {verificationBusy ? "Sending..." : "Send verification email"}<span aria-hidden="true">↗</span>
            </button>
            <small className="payment-verification-waiting"><i /> Waiting for verification</small>
          </div>
        )}

        {user?.emailVerified && !deliveryLoading && !payment && (
          <>
            <span className="section-kicker">JON. PC / Checkout</span>
            <h2 id="payment-title">Delivery details.</h2>
            <p className="payment-intro">Confirm where your build should arrive.</p>
            <div className="payment-account-line"><span>Signed in as</span><span>{user.email}</span></div>
            <form className="payment-delivery-form" onSubmit={(event) => void confirmDelivery(event)}>
              <div className="payment-delivery-grid">
                <label>Recipient name<input autoComplete="name" required maxLength={160} value={delivery.recipientName} onChange={(event) => setDelivery({ ...delivery, recipientName: event.target.value })} /></label>
                <label>Phone<input autoComplete="tel" type="tel" inputMode="tel" required maxLength={40} value={delivery.phone} onChange={(event) => setDelivery({ ...delivery, phone: event.target.value })} /></label>
              </div>
              <label>Street address<input autoComplete="address-line1" required maxLength={200} value={delivery.addressLine1} onChange={(event) => setDelivery({ ...delivery, addressLine1: event.target.value })} /></label>
              <label>Apartment / unit <small>If applicable</small><input autoComplete="address-line2" maxLength={200} value={delivery.addressLine2} onChange={(event) => setDelivery({ ...delivery, addressLine2: event.target.value })} /></label>
              <div className="payment-delivery-grid payment-delivery-location">
                <label>Suburb<input autoComplete="address-level2" required maxLength={100} value={delivery.suburb} onChange={(event) => setDelivery({ ...delivery, suburb: event.target.value })} /></label>
                <label>State<select autoComplete="address-level1" value={delivery.state} onChange={(event) => setDelivery({ ...delivery, state: event.target.value })}>{["VIC", "NSW", "QLD", "SA", "WA", "TAS", "ACT", "NT"].map((state) => <option key={state}>{state}</option>)}</select></label>
                <label>Postcode<input autoComplete="postal-code" required inputMode="numeric" pattern="[0-9]{4}" maxLength={4} value={delivery.postcode} onChange={(event) => setDelivery({ ...delivery, postcode: event.target.value })} /></label>
              </div>
              {error && <p className="payment-error" role="alert">{error}</p>}
              <button className="button button-primary payment-primary" type="submit" disabled={busy}>{busy ? "Preparing..." : "Continue to payment"}<span aria-hidden="true">↗</span></button>
            </form>
          </>
        )}

        {payment?.status === "CREATED" && (
          <>
            <span className="section-kicker">JON. PC / Demo checkout</span>
            <h2 id="payment-title">Review and pay.</h2>
            <p className="payment-intro">Review your order and current Founder offer before payment.</p>
            <PaymentSummary payment={payment} />
            {payment.rewardPreview?.available && <div className="payment-reward-preview">
              <div className="payment-reward-preview-heading"><span>Founder cashback</span><strong>{formatAudExact(payment.rewardPreview.cashbackAmountCents!)}</strong></div>
              <div className="payment-reward-preview-details">
                <span>{payment.rewardPreview.tierName} · {payment.rewardPreview.rateBasisPoints! / 100}% up to {formatAudExact(payment.rewardPreview.capCents!)}</span>
                <span>Effective price after cashback · {formatAudExact(payment.rewardPreview.effectivePriceCents!)}</span>
              </div>
              <small>Current offer · confirmed when payment succeeds</small>
            </div>}
            {payment.rewardPreview && !payment.rewardPreview.available && <div className="payment-reward-unavailable"><span>Founder cashback</span><p>{payment.rewardPreview.reason}</p></div>}
            {payment.invoiceNumber && <div className="payment-invoice-reference"><span>Tax invoice</span><strong>{payment.invoiceNumber}</strong></div>}
            <div className="payment-assurance">
              <span><i /> Amount confirmed by JON. PC</span>
              <span><i /> Founder cashback shown before payment</span>
            </div>
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={() => complete("SUCCEEDED")} disabled={busy}>{busy ? "Confirming..." : `Complete payment · ${formatAud(payment.amountCents)}`} <span aria-hidden="true">↗</span></button>
            <button className="payment-secondary" type="button" onClick={payLater} disabled={busy}>Pay later</button>
            <small className="payment-demo-note">Local simulation only. No card details are collected or processed.</small>
          </>
        )}

        {payment?.status === "FAILED" && (
          <>
            <span className="section-kicker">Payment not completed</span>
            <h2 id="payment-title">Nothing was charged.</h2>
            <p className="payment-intro">Your order is still saved and no Founder place was claimed.</p>
            <PaymentSummary payment={payment} />
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={() => requestReference && beginCheckout(requestReference, true)} disabled={busy}>{busy ? "Preparing..." : "Try payment again"} <span aria-hidden="true">↗</span></button>
          </>
        )}

        {payment?.status === "SUCCEEDED" && (
          <>
            <span className="section-kicker payment-success-kicker"><i /> Payment confirmed</span>
            <h2 id="payment-title">You&apos;re in.</h2>
            <p className="payment-intro">Your order is confirmed{payment.rewardState === "JOINED" ? " and your Founder cashback is locked." : "."}</p>
            <PaymentSummary payment={payment} />
            {payment.reward && (
              <div className="payment-reward-result">
                <div><span>Founder number</span><strong>#{formatRewardId(payment.reward.founderNumber)}</strong></div>
                <div><span>Reward tier</span><strong>{payment.reward.tierName}</strong></div>
                <div><span>Cashback locked</span><strong>{formatAudExact(payment.reward.cashbackAmountCents)}</strong></div>
              </div>
            )}
            {payment.reward && <div className="payment-reward-timing"><span>Cashback return</span><strong>Original payment method</strong><p>Initiated within 30 days of confirmed delivery. Provider processing times may vary.</p></div>}
            {error && <p className="payment-error" role="alert">{error}</p>}
            {payment.invoiceId && payment.invoiceNumber && <button className="payment-invoice-download" type="button" onClick={downloadInvoice}>Download tax invoice ↓</button>}
            <button className="button button-primary payment-primary" type="button" onClick={viewRewards}>View my rewards <span aria-hidden="true">→</span></button>
          </>
        )}
      </section>
    </div>
  );
}

function PaymentSummary({ payment }: { payment: MockPayment }) {
  return (
    <div className="payment-summary">
      <div><span>Order</span><strong>{payment.orderReference}</strong></div>
      <div><span>Total</span><strong>{formatAud(payment.amountCents)}</strong></div>
      <small>Payment reference {payment.paymentReference}</small>
    </div>
  );
}

function formatAud(amountCents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", maximumFractionDigits: 0 }).format(amountCents / 100);
}

function formatAudExact(amountCents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amountCents / 100);
}

function formatRewardId(founderNumber: number) {
  return String(founderNumber).padStart(2, "0");
}

export default PaymentCheckout;
