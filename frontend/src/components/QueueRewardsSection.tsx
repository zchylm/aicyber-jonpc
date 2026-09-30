import { useCallback, useEffect, useRef, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey } from "../api/auth";
import {
  createFounderDemoOrder,
  fetchMyRewards,
  fetchRewardSummary,
  makeCashbackPayable,
  markCashbackPaid,
  qualifyLatestBuildRequest,
  resetRewardDemo,
  type RewardEntry,
  type RewardMemberSummary,
  type RewardPublicSummary,
} from "../api/rewards";
import { useHistoryPanel } from "../hooks/useHistoryPanel";
import "./QueueRewardsSection.css";

type Props = { user: AuthUser | null };
const demoEnabled = import.meta.env.DEV;

function QueueRewardsSection({ user }: Props) {
  const [summary, setSummary] = useState<RewardPublicSummary | null>(null);
  const [member, setMember] = useState<RewardMemberSummary | null>(null);
  const [isCentreOpen, setIsCentreOpen] = useState(false);
  const [isExplanationOpen, setIsExplanationOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const refreshInFlight = useRef(false);
  const { open: openRewardCentre, close: closeRewardCentre } = useHistoryPanel("founder-rewards", isCentreOpen, setIsCentreOpen);

  const refresh = useCallback(async () => {
    if (refreshInFlight.current) return;
    refreshInFlight.current = true;
    try {
      const token = window.localStorage.getItem(authTokenKey);
      const [nextSummary, nextMember] = await Promise.all([
        fetchRewardSummary(),
        user && token ? fetchMyRewards(token) : Promise.resolve(null),
      ]);
      setSummary(nextSummary);
      setMember(nextMember);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load Founder rewards.");
    } finally {
      refreshInFlight.current = false;
    }
  }, [user]);

  useEffect(() => {
    const timer = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(timer);
  }, [refresh]);
  useEffect(() => {
    const open = () => { openRewardCentre(); void refresh(); };
    window.addEventListener("jonpc:open-rewards", open);
    window.addEventListener("jonpc:payment-completed", refresh);
    return () => {
      window.removeEventListener("jonpc:open-rewards", open);
      window.removeEventListener("jonpc:payment-completed", refresh);
    };
  }, [openRewardCentre, refresh]);

  async function runDemo(action: "qualify" | "create" | "payable" | "paid" | "reset") {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return setError("Log in before running the local demo.");
    if (action === "reset" && !window.confirm("Reset local Founder campaign data and restore featured-system test stock?")) return;
    setBusy(true); setError(null); setMessage(null);
    try {
      const response = action === "qualify" ? await qualifyLatestBuildRequest(token)
        : action === "create" ? await createFounderDemoOrder(token)
          : action === "payable" ? await makeCashbackPayable(token)
            : action === "paid" ? await markCashbackPaid(token) : await resetRewardDemo(token);
      setSummary(response.summary); setMember(response.member); setMessage(response.message);
      if (action !== "create") window.dispatchEvent(new CustomEvent("jonpc:system-stock-changed"));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to run the Founder demo.");
    } finally { setBusy(false); }
  }

  const entries = member?.entries ?? [];
  const latest = entries[0] ?? null;
  const rate = summary?.currentRateBasisPoints ? `${summary.currentRateBasisPoints / 100}%` : "—";

  return <>
    <section className="reward-section" id="queue-rewards" aria-labelledby="reward-title">
      <div className="reward-banner">
        <div className="reward-banner-copy">
          <span className="section-kicker reward-kicker"><i aria-hidden="true" />JON. PC Founders</span>
          <h2 id="reward-title">Join early. <em>Get rewarded.</em></h2>
          <p>Your Founder cashback is calculated and locked when payment is confirmed.</p>
          <div className="reward-banner-actions">
            <button className="button button-primary" type="button" onClick={openRewardCentre}>My Founder reward <span aria-hidden="true">→</span></button>
            <button className="reward-text-link" type="button" onClick={() => setIsExplanationOpen((open) => !open)}>{isExplanationOpen ? "Close details" : "How it works"} <span aria-hidden="true">{isExplanationOpen ? "−" : "+"}</span></button>
          </div>
        </div>
        <div className="reward-live" aria-label="Founder campaign overview">
          <div className="reward-live-heading"><span>Founder release</span><b>{summary?.programStatus === "ACTIVE" ? "Open" : "Closed"}</b></div>
          <div className="reward-live-metrics">
            <div><span>Claimed</span><strong>{summary ? `${summary.confirmedCount}/${summary.maxPositions}` : "—"}</strong></div>
            <div><span>Current reward</span><strong>{rate}</strong></div>
            <div><span>Maximum</span><strong>{summary?.currentCapCents != null ? formatAud(summary.currentCapCents) : "—"}</strong></div>
          </div>
          <div className="reward-position-card reward-position-card-active">
            <div className="reward-position-heading"><div><span>{latest ? "Your Founder number" : "Positions remaining"}</span><strong>{latest ? `#${pad(latest.founderNumber)}` : summary?.remainingPositions ?? "—"}</strong></div><button type="button" onClick={openRewardCentre}>View <i aria-hidden="true">→</i></button></div>
            {latest && <div className="reward-position-amounts"><div><span>Tier</span><strong>{latest.tierName}</strong></div><div><span>Cashback</span><strong>{formatAud(latest.cashbackAmountCents)}</strong></div><div><span>Status</span><strong>{title(latest.status)}</strong></div></div>}
          </div>
        </div>
      </div>

      {isExplanationOpen && <div className="reward-explanation" id="reward-explanation">
        <div><span className="section-kicker">Limited Founder release</span><h3>Clear from the start.</h3></div>
        <div className="reward-explanation-grid">
          <p><strong>01 / Choose</strong>Select the JON. PC that fits your needs.</p>
          <p><strong>02 / Confirm</strong>Your place is assigned only after successful payment.</p>
          <p><strong>03 / Lock</strong>Cashback equals the current rate of the purchase price before GST, up to the tier cap.</p>
          <p><strong>04 / Receive</strong>Your locked amount does not depend on another customer purchasing.</p>
        </div>
        <aside className="reward-explanation-limit"><strong>One Founder reward per customer</strong><p>Founder positions are confirmed in successful-payment order.</p></aside>
      </div>}

      <div className="reward-rules">
        <article><span>01 / LAUNCH</span><h3>15% · up to A$500</h3><small>Founder #01–10</small></article>
        <article><span>02 / EARLY</span><h3>12% · up to A$500</h3><small>Founder #11–25</small></article>
        <article><span>03 / FOUNDER</span><h3>10% · up to A$500</h3><small>Founder #26–50</small></article>
        <article><span>04 / CLOSE</span><h3>The pilot ends.</h3><small>After Founder #50</small></article>
      </div>

      {demoEnabled && <details className="reward-demo-tools"><summary><span>Local demo tools</span><b>Open controls <i aria-hidden="true">+</i></b></summary><div className="reward-demo-body"><div className="reward-demo-copy"><strong>Development workflow</strong><span>Test the complete cashback lifecycle.</span></div><div className="reward-demo-actions"><button type="button" onClick={() => runDemo("qualify")} disabled={busy || !user}>Create paid demo order</button><button type="button" onClick={() => runDemo("create")} disabled={busy || !user}>Claim next Founder place</button><button type="button" onClick={() => runDemo("payable")} disabled={busy || !user || latest?.status !== "LOCKED"}>Make cashback payable</button><button type="button" onClick={() => runDemo("paid")} disabled={busy || !user || !latest || !["PAYABLE", "PROCESSING"].includes(latest.status)}>Mark cashback paid</button><button className="reward-demo-reset" type="button" onClick={() => runDemo("reset")} disabled={busy || !user}>Reset local demo</button></div>{message && <p className="reward-demo-message">{message}</p>}{error && <p className="reward-demo-error" role="alert">{error}</p>}</div></details>}
    </section>

    {isCentreOpen && <div className="reward-centre-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeRewardCentre(); }}><section className="reward-centre" role="dialog" aria-modal="true" aria-labelledby="reward-centre-title"><button className="reward-centre-close" type="button" onClick={closeRewardCentre} aria-label="Close reward centre">×</button><span className="section-kicker">JON. PC / Founders</span><h2 id="reward-centre-title">Your Founder rewards.</h2>{!user && <p className="reward-centre-empty">Sign in to view your reward.</p>}{user && entries.length === 0 && <p className="reward-centre-empty">No Founder reward yet.</p>}{entries.map((entry) => <FounderCard entry={entry} key={entry.id} />)}</section></div>}
  </>;
}

function FounderCard({ entry }: { entry: RewardEntry }) {
  return <article className="reward-order-card"><div className="reward-order-heading"><div className="reward-order-identity"><span>Founder</span><strong>#{pad(entry.founderNumber)}</strong><small>{entry.tierName} · Order {entry.orderReference}</small></div><span className="reward-order-status">{rewardStatus(entry.status)}</span></div><div className="reward-order-amounts"><div><span>Price paid today</span><strong>{formatAud(entry.purchaseAmountCents)}</strong></div><div><span>Founder cashback</span><strong>{formatAud(entry.cashbackAmountCents)}</strong></div><div><span>Effective price after cashback</span><strong>{formatAud(entry.effectivePriceCents)}</strong></div></div><div className="reward-order-payout"><span>{payoutHeading(entry)}</span><p>{payoutDetail(entry)}</p></div></article>;
}

const formatAud = (cents: number) => new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cents / 100);
const pad = (value: number) => String(value).padStart(2, "0");
const title = (value: string) => value.charAt(0) + value.slice(1).toLowerCase();
const rewardStatus = (value: string) => ({ LOCKED: "Locked", PAYABLE: "Ready for payout", PROCESSING: "Processing", PAID: "Paid", FAILED: "Needs attention", VOID: "Cancelled" } as Record<string, string>)[value] ?? title(value);
const payoutHeading = (entry: RewardEntry) => entry.status === "PAID" ? "Returned to original payment method" : entry.payoutDueAt ? `Scheduled by ${date(entry.payoutDueAt)}` : "Payout timing";
const payoutDetail = (entry: RewardEntry) => entry.status === "PAID"
  ? `${entry.paidAt ? `Paid ${date(entry.paidAt)}` : "Paid"}${entry.payoutReference ? ` · ${entry.payoutReference}` : ""}`
  : entry.status === "FAILED" ? "JON.PC support will review this payout."
    : "JON.PC will initiate eligible cashback within 30 days after confirmed delivery.";
const date = (value: string) => new Intl.DateTimeFormat("en-AU", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));

export default QueueRewardsSection;
