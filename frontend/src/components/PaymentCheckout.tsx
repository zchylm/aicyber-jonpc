import { useEffect, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey, fetchCurrentUser, sendEmailVerification } from "../api/auth";
import { completeMockPayment, createBankTransfer, createMockPayment, createStripePayment, downloadInvoicePdf, fetchBankTransfer, fetchDeliveryDetails, fetchStripePayment, reportBankTransfer, saveDeliveryDetails, type BankTransferPayment, type DeliveryDetails, type MockPayment, type StripePayment } from "../api/payments";
import { bankTransferEnabled, stripeEnabled, stripeTestMode } from "../config/runtime";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import "./PaymentCheckout.css";

type PaymentCheckoutProps = {
  user: AuthUser | null;
  onUserChange: (user: AuthUser) => void;
};

function stripeReturnFromUrl(): { result: "success" | "cancelled"; paymentId: string } | null {
  const params = new URLSearchParams(window.location.search);
  const result = params.get("stripe_result");
  const paymentId = params.get("stripe_payment");
  return (result === "success" || result === "cancelled") && paymentId
    ? { result, paymentId }
    : null;
}

function PaymentCheckout({ user, onUserChange }: PaymentCheckoutProps) {
  const [initialStripeReturn] = useState(stripeReturnFromUrl);
  const [isOpen, setIsOpen] = useState(Boolean(initialStripeReturn));
  const [requestReference, setRequestReference] = useState<string | null>(null);
  const [payment, setPayment] = useState<MockPayment | BankTransferPayment | StripePayment | null>(null);
  const [choosingPayment, setChoosingPayment] = useState(false);
  const [stripeReturn, setStripeReturn] = useState<"success" | "cancelled" | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [verificationBusy, setVerificationBusy] = useState(false);
  const [verificationMessage, setVerificationMessage] = useState<string | null>(null);
  const [restoringStripePayment, setRestoringStripePayment] = useState(Boolean(initialStripeReturn));
  const [transferAcknowledged, setTransferAcknowledged] = useState(false);
  const [delivery, setDelivery] = useState<DeliveryDetails>({ deliveryMethod: "DELIVERY", recipientName: "", phone: "", addressLine1: "", addressLine2: "", suburb: "", state: "VIC", postcode: "" });
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
      setChoosingPayment(false);
      setStripeReturn(null);
      setError(null);
      setVerificationMessage(null);
      setTransferAcknowledged(false);
      setDeliveryLoadedFor(null);
      setDelivery({ deliveryMethod: "DELIVERY", recipientName: user.displayName ?? "", phone: "", addressLine1: "", addressLine2: "", suburb: "", state: "VIC", postcode: "" });
    };
    window.addEventListener("jonpc:open-checkout", openCheckout);
    return () => window.removeEventListener("jonpc:open-checkout", openCheckout);
  }, [openCheckoutPanel, user]);

  useEffect(() => {
    if (!initialStripeReturn || !user?.emailVerified) return;
    const { result, paymentId } = initialStripeReturn;
    let cancelled = false;
    const clean = new URL(window.location.href);
    clean.searchParams.delete("stripe_result");
    clean.searchParams.delete("stripe_payment");
    window.history.replaceState(window.history.state, "", clean);
    const restoreStripePayment = async () => {
      await Promise.resolve();
      if (cancelled) return;
      const token = window.localStorage.getItem(authTokenKey);
      if (!token) {
        setRestoringStripePayment(false);
        setError("Sign in again to confirm this Stripe payment.");
        return;
      }
      setBusy(true);
      setError(null);
      setStripeReturn(result);
      openCheckoutPanel();
      try {
        const response = await fetchStripePayment(token, paymentId);
        if (cancelled) return;
        const savedRequestReference = window.sessionStorage.getItem(`jonpc:stripe-request:${paymentId}`);
        setPayment(response);
        setRequestReference(savedRequestReference);
        if (["SUCCEEDED", "FAILED", "CANCELLED"].includes(response.status) && savedRequestReference) {
          window.sessionStorage.removeItem(`jonpc:stripe:${savedRequestReference}`);
        }
        if (response.status === "SUCCEEDED") {
          window.dispatchEvent(new CustomEvent("jonpc:checkout-committed", { detail: { requestReference: savedRequestReference } }));
          window.dispatchEvent(new CustomEvent("jonpc:payment-completed"));
        }
      } catch (requestError) {
        if (!cancelled) setError(requestError instanceof Error ? requestError.message : "Unable to confirm the Stripe payment.");
      } finally {
        if (!cancelled) {
          setBusy(false);
          setRestoringStripePayment(false);
        }
      }
    };
    void restoreStripePayment();
    return () => { cancelled = true; };
  }, [initialStripeReturn, openCheckoutPanel, user?.emailVerified]);

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

  useEffect(() => {
    if (!isOpen || !payment || payment.status !== "PROCESSING" || !isBankTransfer(payment)) return;
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    let cancelled = false;
    const refresh = async () => {
      try {
        const response = await fetchBankTransfer(token, payment.paymentId);
        if (cancelled) return;
        setPayment(response);
        if (response.status === "SUCCEEDED") {
          window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
          window.dispatchEvent(new CustomEvent("jonpc:payment-completed"));
        }
      } catch {
        // Keep the review screen stable; the user can reopen Orders to refresh later.
      }
    };
    const interval = window.setInterval(() => void refresh(), 5000);
    return () => { cancelled = true; window.clearInterval(interval); };
  }, [isOpen, payment]);

  useEffect(() => {
    if (!isOpen || !payment || !isStripePayment(payment) || stripeReturn !== "success" || !["CREATED", "PROCESSING"].includes(payment.status)) return;
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    let cancelled = false;
    let attempts = 0;
    const refresh = async () => {
      try {
        const response = await fetchStripePayment(token, payment.paymentId);
        if (cancelled) return;
        setPayment(response);
        attempts += 1;
        if (response.status === "SUCCEEDED") {
          window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
          window.dispatchEvent(new CustomEvent("jonpc:payment-completed"));
        } else if (attempts >= 12) {
          setError("Stripe is still confirming the payment. You can safely check My Orders in a moment.");
        }
      } catch {
        attempts += 1;
      }
    };
    const interval = window.setInterval(() => { if (attempts < 12) void refresh(); }, 2000);
    return () => { cancelled = true; window.clearInterval(interval); };
  }, [isOpen, payment, stripeReturn]);

  async function beginBankCheckout(reference: string, retry = false) {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      setError("Sign in again to continue to checkout.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const storageKey = `jonpc:${bankTransferEnabled ? "bank-transfer" : "mock-payment"}:${reference}`;
      const key = retry ? crypto.randomUUID() : window.sessionStorage.getItem(storageKey) ?? crypto.randomUUID();
      window.sessionStorage.setItem(storageKey, key);
      setPayment(bankTransferEnabled
        ? await createBankTransfer(token, reference, key)
        : await createMockPayment(token, reference, key));
      setTransferAcknowledged(false);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to prepare checkout.");
    } finally {
      setBusy(false);
    }
  }

  async function beginStripeCheckout(reference: string, retry = false) {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      setError("Sign in again to continue to checkout.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const storageKey = `jonpc:stripe:${reference}`;
      const key = retry ? crypto.randomUUID() : window.sessionStorage.getItem(storageKey) ?? crypto.randomUUID();
      window.sessionStorage.setItem(storageKey, key);
      const response = await createStripePayment(token, reference, key);
      if (!response.checkoutUrl) throw new Error("Stripe did not return a secure checkout page.");
      window.sessionStorage.setItem(`jonpc:stripe-request:${response.paymentId}`, reference);
      setStripeReturn(null);
      setPayment(response);
    } catch (requestError) {
      window.sessionStorage.removeItem(`jonpc:stripe:${reference}`);
      setError(requestError instanceof Error ? requestError.message : "Unable to prepare secure payment.");
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
      if (stripeEnabled || bankTransferEnabled) setChoosingPayment(true);
      else await beginBankCheckout(requestReference);
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
        window.dispatchEvent(new CustomEvent("jonpc:checkout-committed", { detail: { requestReference } }));
        window.dispatchEvent(new CustomEvent("jonpc:payment-completed"));
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to complete payment.");
    } finally {
      setBusy(false);
    }
  }

  async function markTransferred() {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token || !payment || !isBankTransfer(payment) || !transferAcknowledged) return;
    setBusy(true);
    setError(null);
    try {
      setPayment(await reportBankTransfer(token, payment.paymentId));
      window.dispatchEvent(new CustomEvent("jonpc:checkout-committed", { detail: { requestReference } }));
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to submit the transfer for review.");
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

        {(restoringStripePayment || (deliveryLoading && !payment && user?.emailVerified)) && (
          <div className="payment-loading" aria-live="polite"><i /> {restoringStripePayment ? "Confirming your secure payment..." : "Preparing your secure checkout..."}</div>
        )}

        {!restoringStripePayment && !user?.emailVerified && (
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

        {!restoringStripePayment && user?.emailVerified && !deliveryLoading && !payment && !choosingPayment && (
          <>
            <span className="section-kicker">JON. PC / Checkout</span>
            <h2 id="payment-title">Delivery details.</h2>
            <p className="payment-intro">Choose Melbourne delivery or collect from West Melbourne.</p>
            <div className="payment-account-line"><span>Signed in as</span><span>{user.email}</span></div>
            <form className="payment-delivery-form" onSubmit={(event) => void confirmDelivery(event)}>
              <div className="payment-fulfilment-options" role="radiogroup" aria-label="Delivery method">
                <button className={delivery.deliveryMethod === "DELIVERY" ? "active" : ""} type="button" role="radio" aria-checked={delivery.deliveryMethod === "DELIVERY"} onClick={() => setDelivery({ ...delivery, deliveryMethod: "DELIVERY" })}><strong>Melbourne delivery</strong><span>A$25 · metro postcodes</span></button>
                <button className={delivery.deliveryMethod === "PICKUP" ? "active" : ""} type="button" role="radio" aria-checked={delivery.deliveryMethod === "PICKUP"} onClick={() => setDelivery({ ...delivery, deliveryMethod: "PICKUP" })}><strong>West Melbourne pickup</strong><span>Free · by appointment</span></button>
              </div>
              <div className="payment-delivery-grid">
                <label>Recipient name<input autoComplete="name" required maxLength={160} value={delivery.recipientName} onChange={(event) => setDelivery({ ...delivery, recipientName: event.target.value })} /></label>
                <label>Phone<input autoComplete="tel" type="tel" inputMode="tel" required maxLength={40} value={delivery.phone} onChange={(event) => setDelivery({ ...delivery, phone: event.target.value })} /></label>
              </div>
              {delivery.deliveryMethod === "DELIVERY" ? <>
                <label>Street address<input autoComplete="address-line1" required maxLength={200} value={delivery.addressLine1} onChange={(event) => setDelivery({ ...delivery, addressLine1: event.target.value })} /></label>
                <label>Apartment / unit <small>If applicable</small><input autoComplete="address-line2" maxLength={200} value={delivery.addressLine2} onChange={(event) => setDelivery({ ...delivery, addressLine2: event.target.value })} /></label>
                <div className="payment-delivery-grid payment-delivery-location">
                  <label>Suburb<input autoComplete="address-level2" required maxLength={100} value={delivery.suburb} onChange={(event) => setDelivery({ ...delivery, suburb: event.target.value })} /></label>
                  <label>State<select autoComplete="address-level1" value={delivery.state} onChange={(event) => setDelivery({ ...delivery, state: event.target.value })}>{["VIC", "NSW", "QLD", "SA", "WA", "TAS", "ACT", "NT"].map((state) => <option key={state}>{state}</option>)}</select></label>
                  <label>Postcode<input autoComplete="postal-code" required inputMode="numeric" pattern="[0-9]{4}" maxLength={4} value={delivery.postcode} onChange={(event) => setDelivery({ ...delivery, postcode: event.target.value })} /></label>
                </div>
                <small className="payment-area-note">Outside Melbourne metro? Contact support for a delivery quote.</small>
              </> : <div className="payment-pickup-note"><span>Pickup location</span><strong>205 Kensington Road, West Melbourne VIC 3003</strong><p>We’ll email you when your system is ready to collect.</p></div>}
              {error && <p className="payment-error" role="alert">{error}</p>}
              <button className="button button-primary payment-primary" type="submit" disabled={busy}>{busy ? "Preparing..." : "Continue to payment"}<span aria-hidden="true">↗</span></button>
            </form>
          </>
        )}

        {!restoringStripePayment && user?.emailVerified && !deliveryLoading && !payment && choosingPayment && requestReference && (
          <>
            <span className="section-kicker">JON. PC / Payment</span>
            <h2 id="payment-title">Choose how to pay.</h2>
            <p className="payment-intro">See your original price, 5% Founder discount and final total before you pay.</p>
            <div className="payment-methods">
              {stripeEnabled && <button className="payment-method payment-method-featured" type="button" onClick={() => void beginStripeCheckout(requestReference)} disabled={busy}>
                <span className="payment-method-icon" aria-hidden="true">S</span>
                <span><strong>Pay securely online</strong><small>{stripeTestMode ? "Test mode · No real charge" : "Cards and supported wallets"}</small></span>
                <i aria-hidden="true">→</i>
              </button>}
              {bankTransferEnabled && <button className="payment-method" type="button" onClick={() => void beginBankCheckout(requestReference)} disabled={busy}>
                <span className="payment-method-icon payment-method-bank" aria-hidden="true">ANZ</span>
                <span><strong>Bank transfer</strong><small>Reserved for 24 hours</small></span>
                <i aria-hidden="true">→</i>
              </button>}
            </div>
            <div className="payment-security-note"><i /> Secure checkout · Amount verified by JON. PC</div>
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="payment-secondary" type="button" onClick={() => setChoosingPayment(false)} disabled={busy}>Back to delivery details</button>
          </>
        )}

        {payment?.status === "CREATED" && isBankTransfer(payment) && (
          <>
            <span className="section-kicker">JON. PC / Bank transfer</span>
            <h2 id="payment-title">Complete payment.</h2>
            <p className="payment-intro">Your order is reserved for 24 hours while you complete payment.</p>
            <PaymentSummary payment={payment} />
            <div className="payment-bank-card">
              <div className="payment-bank-heading"><span>Transfer to</span><strong>{payment.bankName}</strong></div>
              <BankDetail label="Account name" value={payment.accountName} />
              <BankDetail label="BSB" value={payment.bsb} copy />
              <BankDetail label="Account number" value={payment.accountNumber} copy />
              <BankDetail label="Payment reference" value={payment.paymentReference} copy highlight />
            </div>
            <div className="payment-transfer-window"><span>Pay before</span><strong>{formatDateTime(payment.expiresAt)}</strong></div>
            {payment.discountCents > 0 && <DiscountConfirmation payment={payment} hold="Reserved for 24 hours" />}
            {error && <p className="payment-error" role="alert">{error}</p>}
            <label className="payment-transfer-confirm"><input type="checkbox" checked={transferAcknowledged} onChange={(event) => setTransferAcknowledged(event.target.checked)} /><span>I&apos;ve completed the bank transfer using reference <strong>{payment.paymentReference}</strong>.</span></label>
            <button className="button button-primary payment-primary" type="button" onClick={markTransferred} disabled={busy || !transferAcknowledged}>{busy ? "Submitting…" : "Submit for review"}<span aria-hidden="true">↗</span></button>
            <button className="payment-secondary" type="button" onClick={payLater} disabled={busy}>Finish later in My Orders</button>
          </>
        )}

        {payment?.status === "CREATED" && isStripePayment(payment) && (
          <>
            <span className="section-kicker">JON. PC / Secure payment</span>
            <h2 id="payment-title">{stripeReturn === "success" ? "Confirming payment." : stripeReturn === "cancelled" ? "Payment paused." : "Review and pay."}</h2>
            <p className="payment-intro">{stripeReturn === "success" ? "Stripe is securely confirming your payment. Keep this window open for a moment." : stripeReturn === "cancelled" ? "Nothing was charged. Continue whenever you’re ready." : "Your 5% Founder discount is already included in the total below."}</p>
            <PaymentSummary payment={payment} />
            {payment.discountCents > 0 && <DiscountConfirmation payment={payment} hold="Reserved while this checkout is open" />}
            {error && <p className="payment-error" role="alert">{error}</p>}
            {stripeReturn !== "success" && payment.checkoutUrl && <button className="button button-primary payment-primary" type="button" onClick={() => window.location.assign(payment.checkoutUrl!)}>{stripeReturn === "cancelled" ? "Return to secure payment" : "Continue to secure payment"} <span aria-hidden="true">↗</span></button>}
            <button className="payment-secondary" type="button" onClick={payLater}>View My Orders</button>
          </>
        )}

        {payment?.status === "CREATED" && !isBankTransfer(payment) && !isStripePayment(payment) && (
          <>
            <span className="section-kicker">JON. PC / Demo checkout</span>
            <h2 id="payment-title">Review and pay.</h2>
            <p className="payment-intro">Review your order and current Founder offer before payment.</p>
            <PaymentSummary payment={payment} />
            <div className="payment-build-window"><span>Build & dispatch</span><strong>7–14 business days after cleared payment.</strong></div>
            {payment.discountCents > 0 && <DiscountConfirmation payment={payment} hold="Applied to this checkout" />}
            {payment.invoiceNumber && <div className="payment-invoice-reference"><span>Tax invoice</span><strong>{payment.invoiceNumber}</strong></div>}
            <div className="payment-assurance">
              <span><i /> Amount confirmed by JON. PC</span>
              <span><i /> Founder discount applied before payment</span>
            </div>
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={() => complete("SUCCEEDED")} disabled={busy}>{busy ? "Confirming..." : `Complete payment · ${formatAud(payment.amountCents)}`} <span aria-hidden="true">↗</span></button>
            <button className="payment-secondary" type="button" onClick={payLater} disabled={busy}>Pay later</button>
            <small className="payment-demo-note">Internal test simulation only. No card details are collected or processed.</small>
          </>
        )}

        {payment?.status === "PROCESSING" && isBankTransfer(payment) && (
          <>
            <span className="section-kicker payment-review-kicker"><i /> Transfer submitted</span>
            <h2 id="payment-title">Bank review in progress.</h2>
            <p className="payment-intro">Your order remains reserved while we verify your payment. This usually takes up to 1 business day. Thank you for your patience.</p>
            <PaymentSummary payment={payment} />
            <div className="payment-review-state"><span>Current status</span><strong>Awaiting ANZ verification</strong></div>
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={payLater}>View my orders <span aria-hidden="true">→</span></button>
          </>
        )}

        {payment?.status === "PROCESSING" && isStripePayment(payment) && (
          <>
            <span className="section-kicker payment-review-kicker"><i /> Payment processing</span>
            <h2 id="payment-title">Almost there.</h2>
            <p className="payment-intro">Stripe is confirming your payment. Your order will update automatically.</p>
            <PaymentSummary payment={payment} />
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={payLater}>View My Orders <span aria-hidden="true">→</span></button>
          </>
        )}

        {payment?.status === "FAILED" && (
          <>
            <span className="section-kicker">Payment not completed</span>
            <h2 id="payment-title">Nothing was charged.</h2>
            <p className="payment-intro">{isBankTransfer(payment) && payment.failureReason ? `${payment.failureReason}. Your order is still saved and no Founder place was claimed.` : "Your order is still saved and no Founder place was claimed."}</p>
            <PaymentSummary payment={payment} />
            {error && <p className="payment-error" role="alert">{error}</p>}
            {isStripePayment(payment)
              ? <button className="button button-primary payment-primary" type="button" onClick={() => requestReference && beginStripeCheckout(requestReference, true)} disabled={busy}>{busy ? "Preparing..." : "Try payment again"} <span aria-hidden="true">↗</span></button>
              : <button className="button button-primary payment-primary" type="button" onClick={() => requestReference && beginBankCheckout(requestReference, true)} disabled={busy}>{busy ? "Preparing..." : "Try payment again"} <span aria-hidden="true">↗</span></button>}
          </>
        )}

        {payment?.status === "CANCELLED" && (
          <>
            <span className="section-kicker">Payment window closed</span>
            <h2 id="payment-title">Reservation released.</h2>
            <p className="payment-intro">{isStripePayment(payment)
              ? "The secure payment session ended before payment was completed. Nothing was charged and no Founder place was assigned."
              : "The 24-hour payment window ended before the transfer was submitted. Your reservation was released and no Founder place was assigned."}</p>
            <PaymentSummary payment={payment} />
            {isStripePayment(payment)
              ? <button className="button button-primary payment-primary" type="button" onClick={() => requestReference && beginStripeCheckout(requestReference, true)} disabled={busy}>{busy ? "Preparing..." : "Start a new secure payment"} <span aria-hidden="true">↗</span></button>
              : <button className="button button-primary payment-primary" type="button" onClick={payLater}>View my orders <span aria-hidden="true">→</span></button>}
          </>
        )}

        {payment?.status === "SUCCEEDED" && (
          <>
            <span className="section-kicker payment-success-kicker"><i /> Payment confirmed</span>
            <h2 id="payment-title">You&apos;re in.</h2>
            <p className="payment-intro">Your order is confirmed{payment.discountCents > 0 ? ` and you saved ${formatAudExact(payment.discountCents)}.` : "."} We’ll dispatch within 7–14 business days.</p>
            <PaymentSummary payment={payment} />
            {payment.discountCents > 0 && (
              <div className="payment-reward-result">
                <div><span>Monthly place</span><strong>Confirmed</strong></div>
                <div><span>Founder discount</span><strong>5% off</strong></div>
                <div><span>You saved</span><strong>{formatAudExact(payment.discountCents)}</strong></div>
              </div>
            )}
            {error && <p className="payment-error" role="alert">{error}</p>}
            {payment.invoiceId && payment.invoiceNumber && <button className="payment-invoice-download" type="button" onClick={downloadInvoice}>Download tax invoice ↓</button>}
            <button className="button button-primary payment-primary" type="button" onClick={viewRewards}>View my savings <span aria-hidden="true">→</span></button>
          </>
        )}
      </section>
    </div>
  );
}

function PaymentSummary({ payment }: { payment: MockPayment | BankTransferPayment | StripePayment }) {
  return (
    <div className="payment-summary">
      <div className="payment-summary-reference"><span>Order</span><strong>{payment.orderReference}</strong></div>
      <div className="payment-summary-total"><span>Total</span><strong>{formatAudExact(payment.amountCents)}</strong></div>
      <div className="payment-summary-breakdown">
        <span><i>Product subtotal</i><b>{formatAudExact(payment.productSubtotalCents)}</b></span>
        {payment.discountCents > 0 && <span className="payment-summary-discount"><i>Founder discount · 5% off</i><b>−{formatAudExact(payment.discountCents)}</b></span>}
        <span><i>{payment.shippingFeeCents > 0 ? "Melbourne delivery" : "West Melbourne pickup"}</i><b>{payment.shippingFeeCents > 0 ? formatAudExact(payment.shippingFeeCents) : "Free"}</b></span>
      </div>
    </div>
  );
}

function DiscountConfirmation({ payment, hold }: { payment: MockPayment | BankTransferPayment | StripePayment; hold: string }) {
  return <div className="payment-reward-preview"><div className="payment-reward-preview-heading"><span>Founder 5% discount</span><strong>−{formatAudExact(payment.discountCents)}</strong></div><div className="payment-reward-preview-details"><span>Applied to the product subtotal · delivery excluded</span><span>{hold}</span></div></div>;
}

function BankDetail({ label, value, copy = false, highlight = false }: { label: string; value: string; copy?: boolean; highlight?: boolean }) {
  const [copied, setCopied] = useState(false);
  async function copyValue() {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1500);
  }
  return <div className={`payment-bank-detail${highlight ? " highlight" : ""}`}><span>{label}</span><strong>{value}</strong>{copy && <button type="button" onClick={() => void copyValue()}>{copied ? "Copied" : "Copy"}</button>}</div>;
}

function isBankTransfer(payment: MockPayment | BankTransferPayment | StripePayment): payment is BankTransferPayment {
  return "bankName" in payment;
}

function isStripePayment(payment: MockPayment | BankTransferPayment | StripePayment): payment is StripePayment {
  return "checkoutUrl" in payment;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en-AU", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function formatAud(amountCents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", maximumFractionDigits: 0 }).format(amountCents / 100);
}

function formatAudExact(amountCents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amountCents / 100);
}

export default PaymentCheckout;
