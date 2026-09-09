import { useCallback, useEffect, useRef, useState } from "react";
import type { AuthUser } from "../api/auth";
import { authTokenKey } from "../api/auth";
import {
  fetchMyRewards,
  fetchRewardSummary,
  qualifyLatestBuildRequest,
  resetRewardDemo,
  simulateIncomingOrder,
  type RewardEntry,
  type RewardMemberSummary,
  type RewardPublicSummary,
} from "../api/rewards";
import "./QueueRewardsSection.css";

type QueueRewardsSectionProps = {
  user: AuthUser | null;
};

const demoEnabled = import.meta.env.DEV;
const rewardRefreshIntervalMs = 5_000;

function QueueRewardsSection({ user }: QueueRewardsSectionProps) {
  const [summary, setSummary] = useState<RewardPublicSummary | null>(null);
  const [member, setMember] = useState<RewardMemberSummary | null>(null);
  const [memberOwnerId, setMemberOwnerId] = useState<string | null>(null);
  const [isCentreOpen, setIsCentreOpen] = useState(false);
  const [memberError, setMemberError] = useState<string | null>(null);
  const [demoBusy, setDemoBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const refreshInFlight = useRef(false);

  const loadSummary = useCallback(() => {
    return fetchRewardSummary().then(setSummary).catch(() => undefined);
  }, []);

  const loadMember = useCallback((currentUser: AuthUser) => {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    return fetchMyRewards(token)
      .then((response) => {
        setMember(response);
        setMemberOwnerId(currentUser.id);
        setMemberError(null);
      })
      .catch((requestError) => {
        setMemberOwnerId(currentUser.id);
        setMemberError(requestError instanceof Error ? requestError.message : "Unable to load your rewards.");
      });
  }, []);

  const refreshRewards = useCallback(async () => {
    if (refreshInFlight.current) return;
    refreshInFlight.current = true;
    try {
      await Promise.all([
        loadSummary(),
        user ? loadMember(user) : Promise.resolve(),
      ]);
    } finally {
      refreshInFlight.current = false;
    }
  }, [loadMember, loadSummary, user]);

  useEffect(() => {
    void loadSummary();
  }, [loadSummary]);

  useEffect(() => {
    if (!user) return;
    void loadMember(user);
  }, [loadMember, user]);

  useEffect(() => {
    const openCentre = () => {
      setIsCentreOpen(true);
      if (user) void loadMember(user);
    };
    window.addEventListener("jonpc:open-rewards", openCentre);
    return () => window.removeEventListener("jonpc:open-rewards", openCentre);
  }, [loadMember, user]);

  useEffect(() => {
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") void refreshRewards();
    };
    const refreshTimer = window.setInterval(refreshWhenVisible, rewardRefreshIntervalMs);
    window.addEventListener("focus", refreshWhenVisible);
    document.addEventListener("visibilitychange", refreshWhenVisible);
    window.addEventListener("jonpc:payment-completed", refreshWhenVisible);
    return () => {
      window.clearInterval(refreshTimer);
      window.removeEventListener("focus", refreshWhenVisible);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
      window.removeEventListener("jonpc:payment-completed", refreshWhenVisible);
    };
  }, [refreshRewards]);

  useEffect(() => {
    if (!isCentreOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setIsCentreOpen(false);
    };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [isCentreOpen]);

  async function runDemo(action: "qualify" | "simulate" | "reset") {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      setError("Log in before running the local reward demo.");
      return;
    }
    if (action === "reset" && !window.confirm("Reset the local reward queue and demo Sales Orders? Your account and submitted configurations will remain.")) return;

    setDemoBusy(true);
    setMessage(null);
    setError(null);
    try {
      const response = action === "qualify"
        ? await qualifyLatestBuildRequest(token)
        : action === "simulate"
          ? await simulateIncomingOrder(token)
          : await resetRewardDemo(token);
      setSummary(response.summary);
      setMember(response.member);
      setMemberOwnerId(user?.id ?? null);
      setMessage(response.message);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to run the reward demo.");
    } finally {
      setDemoBusy(false);
    }
  }

  const memberIsCurrent = Boolean(user && memberOwnerId === user.id);
  const memberLoading = Boolean(user && !memberIsCurrent && !memberError);
  const entries = memberIsCurrent ? member?.entries ?? [] : [];
  const activeEntries = entries.filter((entry) => entry.status === "WAITING");
  const completedEntries = entries.filter((entry) => entry.status === "COMPLETED");
  const primaryActiveEntry = [...activeEntries].sort((left, right) => (left.currentPosition ?? Number.MAX_SAFE_INTEGER) - (right.currentPosition ?? Number.MAX_SAFE_INTEGER))[0] ?? null;
  const latestCompletedEntry = completedEntries[0] ?? null;
  const totalMemberAllocated = entries.reduce((total, entry) => total + entry.allocatedAmountCents, 0);

  return (
    <>
      <section className="reward-section" id="queue-rewards" aria-labelledby="reward-title">
        <div className="reward-banner">
          <div className="reward-banner-copy">
            <span className="section-kicker reward-kicker"><i aria-hidden="true" />JON. Queue Rewards</span>
            <h2 id="reward-title">Your build can keep <em>giving back.</em></h2>
            <p>Every eligible order joins the queue. New orders reward customers ahead.</p>
            <div className="reward-banner-actions">
              <a className="button button-primary" href="#reward-rules">How it works <span aria-hidden="true">↘</span></a>
              <button className="reward-text-link" type="button" onClick={() => window.dispatchEvent(new CustomEvent("jonpc:open-rewards"))}>My rewards <span aria-hidden="true">→</span></button>
            </div>
          </div>

          <div className="reward-live" aria-label="Queue overview">
            <div className="reward-live-heading"><span>Live reward queue</span><b>{summary?.available ? "Live database" : "Ready"}</b></div>
            <div className="reward-live-metrics">
              <div><span>Waiting</span><strong>{summary?.waitingCount ?? "—"}</strong></div>
              <div><span>Completed</span><strong>{summary?.completedCount ?? "—"}</strong></div>
              <div><span>Total allocated</span><strong>{summary?.available ? formatAud(summary.totalAllocatedCents) : "—"}</strong></div>
            </div>
            <QueuePositionCard
              user={user}
              loading={memberLoading}
              activeEntry={primaryActiveEntry}
              completedEntry={latestCompletedEntry}
            />
          </div>
        </div>

        <div className="reward-rules" id="reward-rules">
          <article><span>01 / BUY</span><h3>Choose your JON. PC.</h3></article>
          <article><span>02 / JOIN</span><h3>Eligible orders join.</h3></article>
          <article><span>03 / CONTRIBUTE</span><h3>New orders fund rewards.</h3></article>
          <article><span>04 / FIFO</span><h3>Rewards begin at the front.</h3></article>
        </div>

        {demoEnabled && (
          <details className="reward-demo-tools">
            <summary><span>Local demo tools</span><b>Open controls <i aria-hidden="true">+</i></b></summary>
            <div className="reward-demo-body">
              <div className="reward-demo-copy"><strong>Development workflow</strong><span>Create an entry, move the queue, or start again.</span></div>
              <div className="reward-demo-actions">
                <button type="button" onClick={() => runDemo("qualify")} disabled={demoBusy || !user}>Create paid demo order</button>
                <button type="button" onClick={() => runDemo("simulate")} disabled={demoBusy || !user || entries.length === 0}>Add $2,000 incoming order</button>
                <button className="reward-demo-reset" type="button" onClick={() => runDemo("reset")} disabled={demoBusy || !user}>Reset local demo</button>
              </div>
              {message && <p className="reward-demo-message" aria-live="polite">{message}</p>}
              {error && <p className="reward-demo-error" role="alert">{error}</p>}
            </div>
          </details>
        )}
      </section>

      {isCentreOpen && (
        <div className="reward-centre-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setIsCentreOpen(false); }}>
          <section className="reward-centre" role="dialog" aria-modal="true" aria-labelledby="reward-centre-title">
            <button className="reward-centre-close" type="button" onClick={() => setIsCentreOpen(false)} aria-label="Close reward centre">×</button>
            <span className="section-kicker">JON. PC / Reward centre</span>
            <h2 id="reward-centre-title">Your rewards.</h2>
            {user && <p className="reward-centre-welcome">Welcome back, <strong>{user.displayName}</strong>.</p>}

            {!user && <RewardCentreEmpty title="Sign in to see your rewards." />}
            {user && memberLoading && <RewardCentreEmpty title="Loading your reward position..." />}
            {user && !memberLoading && memberError && entries.length === 0 && <RewardCentreEmpty title={memberError} />}
            {user && !memberLoading && !memberError && entries.length === 0 && <RewardCentreEmpty title="No reward orders yet." />}

            {user && !memberLoading && entries.length > 0 && (
              <>
                <div className="reward-centre-summary">
                  <div><span>Total allocated</span><strong>{formatAud(totalMemberAllocated)}</strong></div>
                  <div><span>Active orders</span><strong>{activeEntries.length}</strong></div>
                </div>

                {activeEntries.length > 0 && (
                  <div className="reward-centre-group">
                    <h3>Active</h3>
                    {activeEntries.map((entry) => <RewardOrderCard entry={entry} key={entry.id} />)}
                  </div>
                )}

                {completedEntries.length > 0 && (
                  <div className="reward-centre-group">
                    <h3>Completed</h3>
                    {completedEntries.map((entry) => <RewardOrderCard entry={entry} key={entry.id} />)}
                  </div>
                )}
                <p className="reward-centre-note">Allocated rewards are recorded in the queue. Payouts will be shown separately when available.</p>
              </>
            )}
          </section>
        </div>
      )}
    </>
  );
}

function RewardOrderCard({ entry }: { entry: RewardEntry }) {
  const complete = entry.status === "COMPLETED";
  return (
    <article className="reward-order-card">
      <div className="reward-order-heading">
        <div className="reward-order-identity">
          <span>Reward ID</span>
          <strong>#{formatRewardId(entry.queueSequence)}</strong>
          <small>Order {entry.orderReference}</small>
        </div>
        <span className="reward-order-status">{complete ? "Completed" : `Position #${entry.currentPosition ?? "—"}`}</span>
      </div>
      <div className="reward-order-amounts">
        <div><span>Allocated</span><strong>{formatAud(entry.allocatedAmountCents)}</strong></div>
        <div><span>Remaining</span><strong>{formatAud(entry.remainingAmountCents)}</strong></div>
        <div><span>Total target</span><strong>{formatAud(entry.targetAmountCents)}</strong></div>
      </div>
      <div className="reward-progress" role="progressbar" aria-label={`${entry.orderReference} reward progress`} aria-valuenow={entry.progressPercent} aria-valuemin={0} aria-valuemax={100}><i style={{ width: `${entry.progressPercent}%` }} /></div>
      <small className="reward-order-progress-label">{entry.progressPercent}% complete</small>
    </article>
  );
}

function QueuePositionCard({
  user,
  loading,
  activeEntry,
  completedEntry,
}: {
  user: AuthUser | null;
  loading: boolean;
  activeEntry: RewardEntry | null;
  completedEntry: RewardEntry | null;
}) {
  const label = activeEntry ? "Your queue position" : completedEntry ? "Latest reward" : "Your queue position";
  const value = !user ? "Sign in to view" : loading ? "Checking..." : activeEntry ? `#${activeEntry.currentPosition ?? "—"}` : completedEntry ? "Complete" : "No active order";

  return (
    <div className={activeEntry ? "reward-position-card reward-position-card-active" : "reward-position-card"}>
      <div className="reward-position-heading">
        <div><span>{label}</span><strong>{value}</strong></div>
        <button type="button" onClick={() => window.dispatchEvent(new CustomEvent("jonpc:open-rewards"))}>View all <i aria-hidden="true">→</i></button>
      </div>
      {activeEntry && (
        <>
          <div className="reward-position-detail"><b>Reward ID #{formatRewardId(activeEntry.queueSequence)}</b><span>{activeEntry.progressPercent}% complete</span></div>
          <div className="reward-position-amounts">
            <div><span>Allocated</span><strong>{formatAud(activeEntry.allocatedAmountCents)}</strong></div>
            <div><span>Remaining</span><strong>{formatAud(activeEntry.remainingAmountCents)}</strong></div>
            <div><span>Total target</span><strong>{formatAud(activeEntry.targetAmountCents)}</strong></div>
          </div>
          <div className="reward-progress" role="progressbar" aria-label="Your reward progress" aria-valuenow={activeEntry.progressPercent} aria-valuemin={0} aria-valuemax={100}><i style={{ width: `${activeEntry.progressPercent}%` }} /></div>
        </>
      )}
      {!activeEntry && completedEntry && (
        <>
          <div className="reward-position-detail"><b>Reward ID #{formatRewardId(completedEntry.queueSequence)}</b><span>100% complete</span></div>
          <div className="reward-position-amounts">
            <div><span>Allocated</span><strong>{formatAud(completedEntry.allocatedAmountCents)}</strong></div>
            <div><span>Remaining</span><strong>{formatAud(completedEntry.remainingAmountCents)}</strong></div>
            <div><span>Total target</span><strong>{formatAud(completedEntry.targetAmountCents)}</strong></div>
          </div>
        </>
      )}
    </div>
  );
}

function RewardCentreEmpty({ title }: { title: string }) {
  return <p className="reward-centre-empty">{title}</p>;
}

function formatAud(amountCents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", maximumFractionDigits: 0 }).format(amountCents / 100);
}

function formatRewardId(queueSequence: number) {
  return String(queueSequence).padStart(6, "0");
}

export default QueueRewardsSection;
