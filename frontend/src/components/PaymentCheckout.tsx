import { useEffect, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey } from "../api/auth";
import { completeMockPayment, createMockPayment, type MockPayment } from "../api/payments";
import "./PaymentCheckout.css";

type PaymentCheckoutProps = {
  user: AuthUser | null;
};

function PaymentCheckout({ user }: PaymentCheckoutProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [requestReference, setRequestReference] = useState<string | null>(null);
  const [payment, setPayment] = useState<MockPayment | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const openCheckout = (event: Event) => {
      const reference = (event as CustomEvent<{ requestReference?: string }>).detail?.requestReference;
      if (!reference) return;
      if (!user) {
        window.dispatchEvent(new CustomEvent("jonpc:open-account"));
        return;
      }
      setIsOpen(true);
      setRequestReference(reference);
      setPayment(null);
      setError(null);
      void beginCheckout(reference);
    };
    window.addEventListener("jonpc:open-checkout", openCheckout);
    return () => window.removeEventListener("jonpc:open-checkout", openCheckout);
  }, [user]);

  useEffect(() => {
    if (!isOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !busy) setIsOpen(false);
    };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [busy, isOpen]);

  async function beginCheckout(reference: string) {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      setError("Sign in again to continue to checkout.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      setPayment(await createMockPayment(token, reference, crypto.randomUUID()));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to prepare checkout.");
    } finally {
      setBusy(false);
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
    setIsOpen(false);
    window.dispatchEvent(new CustomEvent("jonpc:open-rewards"));
  }

  if (!isOpen) return null;

  return (
    <div className="payment-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) setIsOpen(false); }}>
      <section className="payment-modal" role="dialog" aria-modal="true" aria-labelledby="payment-title">
        <button className="payment-close" type="button" onClick={() => setIsOpen(false)} disabled={busy} aria-label="Close checkout">×</button>

        {busy && !payment && (
          <div className="payment-loading" aria-live="polite"><i /> Preparing your secure checkout...</div>
        )}

        {!busy && !payment && (
          <>
            <span className="section-kicker">JON. PC / Checkout</span>
            <h2 id="payment-title">Checkout needs attention.</h2>
            <p className="payment-intro">{error ?? "We could not prepare this order."}</p>
            {requestReference && <button className="button button-primary payment-primary" type="button" onClick={() => beginCheckout(requestReference)}>Try again <span aria-hidden="true">↗</span></button>}
          </>
        )}

        {payment?.status === "CREATED" && (
          <>
            <span className="section-kicker">JON. PC / Demo checkout</span>
            <h2 id="payment-title">Review and pay.</h2>
            <p className="payment-intro">One final check before your order joins JON. Queue Rewards.</p>
            <PaymentSummary payment={payment} />
            <div className="payment-assurance">
              <span><i /> Amount confirmed by JON. PC</span>
              <span><i /> Reward eligibility starts after payment</span>
            </div>
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={() => complete("SUCCEEDED")} disabled={busy}>{busy ? "Confirming..." : `Complete demo payment · ${formatAud(payment.amountCents)}`} <span aria-hidden="true">↗</span></button>
            <button className="payment-secondary" type="button" onClick={() => complete("FAILED")} disabled={busy}>Test a declined payment</button>
            <small className="payment-demo-note">Local simulation only. No card details are collected or processed.</small>
          </>
        )}

        {payment?.status === "FAILED" && (
          <>
            <span className="section-kicker">Payment not completed</span>
            <h2 id="payment-title">Nothing was charged.</h2>
            <p className="payment-intro">Your order is still saved and has not entered the reward queue.</p>
            <PaymentSummary payment={payment} />
            {error && <p className="payment-error" role="alert">{error}</p>}
            <button className="button button-primary payment-primary" type="button" onClick={() => requestReference && beginCheckout(requestReference)} disabled={busy}>{busy ? "Preparing..." : "Try payment again"} <span aria-hidden="true">↗</span></button>
          </>
        )}

        {payment?.status === "SUCCEEDED" && (
          <>
            <span className="section-kicker payment-success-kicker"><i /> Payment confirmed</span>
            <h2 id="payment-title">You&apos;re in.</h2>
            <p className="payment-intro">Your order is confirmed{payment.rewardState === "JOINED" ? " and your reward position is ready." : ". Your reward position is being prepared."}</p>
            <PaymentSummary payment={payment} />
            {payment.reward && (
              <div className="payment-reward-result">
                <div><span>Reward ID</span><strong>#{formatRewardId(payment.reward.queueSequence)}</strong></div>
                <div><span>Queue position</span><strong>#{payment.reward.currentPosition ?? "—"}</strong></div>
                <div><span>Total target</span><strong>{formatAud(payment.reward.targetAmountCents)}</strong></div>
              </div>
            )}
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

function formatRewardId(queueSequence: number) {
  return String(queueSequence).padStart(6, "0");
}

export default PaymentCheckout;
