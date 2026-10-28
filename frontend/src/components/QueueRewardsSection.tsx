import { useCallback, useEffect, useRef, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey } from "../api/auth";
import {
  fetchMyRewards,
  fetchRewardSummary,
  type RewardEntry,
  type RewardMemberSummary,
  type RewardPublicSummary,
} from "../api/rewards";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import "./QueueRewardsSection.css";

type Props = { user: AuthUser | null };

function QueueRewardsSection({ user }: Props) {
  const [summary, setSummary] = useState<RewardPublicSummary | null>(null);
  const [member, setMember] = useState<RewardMemberSummary | null>(null);
  const [isCentreOpen, setIsCentreOpen] = useState(false);
  const [isExplanationOpen, setIsExplanationOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const refreshSequence = useRef(0);
  const { open: openRewardCentre, close: closeRewardCentre } = useHistoryPanel("founder-rewards", isCentreOpen, setIsCentreOpen);

  const refresh = useCallback(async () => {
    const sequence = ++refreshSequence.current;
    try {
      const token = window.localStorage.getItem(authTokenKey);
      const [nextSummary, nextMember] = await Promise.all([
        fetchRewardSummary(),
        user && token ? fetchMyRewards(token) : Promise.resolve(null),
      ]);
      if (sequence === refreshSequence.current) {
        setSummary(nextSummary);
        setMember(nextMember);
        setError(null);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load Founder rewards.");
    } finally { /* Newer refreshes supersede older responses. */ }
  }, [user]);

  useEffect(() => {
    const timer = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(timer);
  }, [refresh]);
  useEffect(() => {
    const open = () => { openRewardCentre(); void refresh(); };
    window.addEventListener("jonpc:open-rewards", open);
    window.addEventListener("jonpc:payment-completed", refresh);
    window.addEventListener("jonpc:order-requested", refresh);
    return () => {
      window.removeEventListener("jonpc:open-rewards", open);
      window.removeEventListener("jonpc:payment-completed", refresh);
      window.removeEventListener("jonpc:order-requested", refresh);
    };
  }, [openRewardCentre, refresh]);
  const showRewardCentre = () => { openRewardCentre(); void refresh(); };

  const entries = member?.entries ?? [];
  const latest = entries[0] ?? null;
  const rate = summary?.currentRateBasisPoints ? `${summary.currentRateBasisPoints / 100}%` : "—";

  return <>
    <section className="reward-section" id="queue-rewards" aria-labelledby="reward-title">
      <div className="reward-banner">
        <div className="reward-banner-copy">
          <span className="section-kicker reward-kicker"><i aria-hidden="true" />JON. PC Founders</span>
          <h2 id="reward-title">5% off. <em>Right now.</em></h2>
          <p><strong>50 orders</strong> <strong>every month</strong> receive <strong>5% off</strong> instantly at checkout.</p>
          <div className="reward-banner-actions">
            <button className="button button-primary" type="button" onClick={showRewardCentre}>My Founder savings <span aria-hidden="true">→</span></button>
            <button className="reward-text-link" type="button" onClick={() => setIsExplanationOpen((open) => !open)}>{isExplanationOpen ? "Close details" : "How it works"} <span aria-hidden="true">{isExplanationOpen ? "−" : "+"}</span></button>
          </div>
        </div>
        <div className="reward-live" aria-label="Founder campaign overview">
          <div className="reward-live-heading"><span>This month</span><b>{summary?.programStatus === "ACTIVE" ? "Open" : "Full"}</b></div>
          <div className="reward-live-metrics">
            <div><span>Places used</span><strong>{summary ? `${summary.confirmedCount}/${summary.maxPositions}` : "—"}</strong></div>
            <div><span>Instant discount</span><strong>{rate} off</strong></div>
            <div><span>Places left</span><strong>{summary?.remainingPositions ?? "—"}</strong></div>
          </div>
          <div className="reward-position-card reward-position-card-active">
            <div className="reward-position-heading"><div><span>{latest ? "Your latest saving" : "Available now"}</span><strong>{latest ? formatAud(latest.cashbackAmountCents) : `${summary?.remainingPositions ?? "—"} places`}</strong></div><button type="button" onClick={showRewardCentre}>View <i aria-hidden="true">→</i></button></div>
            {latest && <div className="reward-position-amounts"><div><span>Order</span><strong>{latest.orderReference}</strong></div><div><span>Rate</span><strong>{latest.rateBasisPoints / 100}% off</strong></div><div><span>Status</span><strong>{latest.status === "PROCESSING" ? "Reserved" : "Applied"}</strong></div></div>}
          </div>
        </div>
      </div>

      {isExplanationOpen && <div className="reward-explanation" id="reward-explanation">
        <div><span className="section-kicker">Limited Founder release</span><h3>Clear from the start.</h3></div>
        <div className="reward-explanation-grid">
          <p><strong>01 / Choose</strong>Select the JON. PC that fits your needs.</p>
          <p><strong>02 / Reserve</strong>Your place is held while you complete Stripe checkout or bank transfer.</p>
          <p><strong>03 / Save</strong>5% is deducted from the product subtotal. Delivery is excluded.</p>
          <p><strong>04 / Pay</strong>The original price, discount and final total are all shown before payment.</p>
        </div>
        <aside className="reward-explanation-limit"><strong>One place per eligible order</strong><p>Places reset monthly in Melbourne time and unused places do not roll over.</p></aside>
      </div>}

      <div className="reward-rules">
        <article><span>01 / OFFER</span><h3>5% off now</h3><small>Eligible product subtotal</small></article>
        <article><span>02 / MONTHLY</span><h3>50 orders</h3><small>Resets on the 1st · Melbourne time</small></article>
        <article><span>03 / CLEAR</span><h3>See what you save</h3><small>Original price and discount shown</small></article>
        <article><span>04 / SIMPLE</span><h3>Discount at checkout</h3><small>Applied before payment</small></article>
      </div>

      {error && <p className="reward-demo-error" role="alert">{error}</p>}
    </section>

    {isCentreOpen && <div className="reward-centre-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeRewardCentre(); }}><section className="reward-centre" role="dialog" aria-modal="true" aria-labelledby="reward-centre-title"><button className="reward-centre-close" type="button" onClick={closeRewardCentre} aria-label="Close savings">×</button><span className="section-kicker">JON. PC / Founders</span><h2 id="reward-centre-title">Your Founder savings.</h2>{!user && <p className="reward-centre-empty">Sign in to view your savings.</p>}{user && entries.length === 0 && <p className="reward-centre-empty">No Founder discount used yet.</p>}{entries.map((entry) => <FounderCard entry={entry} key={entry.id} />)}</section></div>}
  </>;
}

function FounderCard({ entry }: { entry: RewardEntry }) {
  return <article className="reward-order-card"><div className="reward-order-heading"><div className="reward-order-identity"><span>Founder discount</span><strong>{entry.rateBasisPoints / 100}% off</strong><small>Order {entry.orderReference}</small></div><span className="reward-order-status">{entry.status === "PROCESSING" ? "Reserved" : "Applied"}</span></div><div className="reward-order-amounts"><div><span>Original total</span><strong>{formatAud(entry.purchaseAmountCents)}</strong></div><div><span>You saved</span><strong>{formatAud(entry.cashbackAmountCents)}</strong></div><div><span>Discounted total</span><strong>{formatAud(entry.effectivePriceCents)}</strong></div></div></article>;
}

const formatAud = (cents: number) => new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cents / 100);

export default QueueRewardsSection;
