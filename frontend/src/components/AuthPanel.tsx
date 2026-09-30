import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import {
  authTokenKey,
  confirmEmailVerification,
  fetchCurrentUser,
  login,
  register,
  requestPasswordReset,
  resetPassword,
  sendEmailVerification,
  type AuthUser,
} from "../api/auth";
import { acceptCustomQuote, cancelOrderRequest, deleteSavedBuild, fetchOrderHistory, fetchSavedBuilds, type OrderHistoryItem, type SavedBuild } from "../api/configurator";
import { downloadInvoicePdf } from "../api/payments";
import { cpuOptions, gpuOptions } from "../data/performance";
import { memoryOptions } from "../data/memory";
import { storageOptions } from "../data/storage";
import { motherboardOptions } from "../data/motherboard";
import { psuOptions } from "../data/psu";
import { caseOptions } from "../data/case";
import { coolingOptions } from "../data/cooling";
import { useHistoryPanel } from "../hooks/useHistoryPanel";

type AuthMode = "login" | "register" | "forgot" | "reset";

type AuthPanelProps = {
  onAuthChange?: (user: AuthUser | null) => void;
};

const paymentDemoEnabled = import.meta.env.DEV;

function AuthPanel({ onAuthChange }: AuthPanelProps) {
  const [initialVerificationToken] = useState(() => new URL(window.location.href).searchParams.get("verifyEmailToken"));
  const [initialPasswordResetToken] = useState(() => new URL(window.location.href).searchParams.get("resetPasswordToken"));
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isOpen, setIsOpen] = useState(Boolean(initialPasswordResetToken));
  const [isAccountOpen, setIsAccountOpen] = useState(false);
  const [isBuildsOpen, setIsBuildsOpen] = useState(false);
  const [isOrdersOpen, setIsOrdersOpen] = useState(false);
  const [savedBuilds, setSavedBuilds] = useState<SavedBuild[]>([]);
  const [orders, setOrders] = useState<OrderHistoryItem[]>([]);
  const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null);
  const [buildsLoading, setBuildsLoading] = useState(false);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState<string | null>(null);
  const [mode, setMode] = useState<AuthMode>(initialPasswordResetToken ? "reset" : "login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [verificationBusy, setVerificationBusy] = useState(false);
  const [verificationFeedback, setVerificationFeedback] = useState<{ message: string; error?: boolean } | null>(null);
  const [recoveryNotice, setRecoveryNotice] = useState<string | null>(null);
  const verificationHandledRef = useRef(false);
  const accountEntryRef = useRef<HTMLDivElement>(null);
  const { open: openAccountPanel, close: closeAccountPanel } = useHistoryPanel("account", isOpen, setIsOpen);
  const { open: openBuildsPanel, close: closeBuildsPanel, dismiss: dismissBuildsPanel } = useHistoryPanel("saved-builds", isBuildsOpen, setIsBuildsOpen);
  const { open: openOrdersPanel, close: closeOrdersPanel, dismiss: dismissOrdersPanel } = useHistoryPanel("orders", isOrdersOpen, setIsOrdersOpen);

  const refreshAccount = useCallback(async () => {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    try {
      const currentUser = await fetchCurrentUser(token);
      setUser(currentUser);
      onAuthChange?.(currentUser);
    } catch {
      window.localStorage.removeItem(authTokenKey);
      setUser(null);
      onAuthChange?.(null);
    }
  }, [onAuthChange]);

  useEffect(() => {
    const initialRefreshId = initialVerificationToken
      ? null
      : window.setTimeout(() => void refreshAccount(), 0);

    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") void refreshAccount();
    };
    const refreshChannel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel("jonpc-auth");
    refreshChannel?.addEventListener("message", refreshAccount);
    window.addEventListener("focus", refreshAccount);
    window.addEventListener("jonpc:auth-refresh", refreshAccount);
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      if (initialRefreshId !== null) window.clearTimeout(initialRefreshId);
      refreshChannel?.close();
      window.removeEventListener("focus", refreshAccount);
      window.removeEventListener("jonpc:auth-refresh", refreshAccount);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [initialVerificationToken, refreshAccount]);

  useEffect(() => {
    if (!initialPasswordResetToken) return;
    const url = new URL(window.location.href);
    url.searchParams.delete("resetPasswordToken");
    window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
  }, [initialPasswordResetToken]);

  useEffect(() => {
    if (!initialVerificationToken || verificationHandledRef.current) return;
    verificationHandledRef.current = true;

    const url = new URL(window.location.href);
    url.searchParams.delete("verifyEmailToken");
    window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
    confirmEmailVerification(initialVerificationToken)
      .then(async () => {
        await refreshAccount();
        setVerificationFeedback(null);
        setIsAccountOpen(true);
        const refreshChannel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel("jonpc-auth");
        refreshChannel?.postMessage("email-verified");
        refreshChannel?.close();
      })
      .catch((requestError) => {
        setVerificationFeedback({
          message: requestError instanceof Error ? requestError.message : "Verification link unavailable.",
          error: true,
        });
        setIsAccountOpen(true);
      });
  }, [initialVerificationToken, refreshAccount]);

  useEffect(() => {
    if (!user) return;
    const loadBuilds = () => {
      const token = window.localStorage.getItem(authTokenKey);
      if (!token) return;
      setBuildsLoading(true);
      fetchSavedBuilds(token).then(setSavedBuilds).catch(() => setSavedBuilds([])).finally(() => setBuildsLoading(false));
    };
    const loadOrders = () => {
      const token = window.localStorage.getItem(authTokenKey);
      if (!token) return;
      setOrdersLoading(true);
      setOrdersError(null);
      fetchOrderHistory(token).then(setOrders).catch((requestError) => {
        setOrders([]);
        setOrdersError(requestError instanceof Error ? requestError.message : "Unable to load order history.");
      }).finally(() => setOrdersLoading(false));
    };
    loadBuilds();
    loadOrders();
    window.addEventListener("jonpc:build-saved", loadBuilds);
    window.addEventListener("jonpc:order-requested", loadOrders);
    return () => {
      window.removeEventListener("jonpc:build-saved", loadBuilds);
      window.removeEventListener("jonpc:order-requested", loadOrders);
    };
  }, [user]);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeAccountPanel();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [closeAccountPanel, isOpen]);

  useEffect(() => {
    if (!isAccountOpen) return;
    const handlePointerDown = (event: PointerEvent) => {
      if (event.target instanceof Node && !accountEntryRef.current?.contains(event.target)) setIsAccountOpen(false);
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setIsAccountOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isAccountOpen]);

  useEffect(() => {
    const openAccount = () => {
      setMode("login");
      setError(null);
      setRecoveryNotice(null);
      openAccountPanel();
    };
    window.addEventListener("jonpc:open-account", openAccount);
    return () => window.removeEventListener("jonpc:open-account", openAccount);
  }, [openAccountPanel]);

  useEffect(() => {
    if (!user) return;
    const openOrders = () => {
      openOrdersPanel();
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
    };
    window.addEventListener("jonpc:open-orders", openOrders);
    return () => window.removeEventListener("jonpc:open-orders", openOrders);
  }, [openOrdersPanel, user]);

  function openPanel(nextMode: AuthMode = "login") {
    setMode(nextMode);
    setError(null);
    setRecoveryNotice(null);
    openAccountPanel();
  }

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setError(null);
    setRecoveryNotice(null);
    setConfirmPassword("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const response = mode === "login"
        ? await login({ email, password })
        : await register({ email, password, displayName });
      window.localStorage.setItem(authTokenKey, response.accessToken);
      setUser(response.user);
      onAuthChange?.(response.user);
      setVerificationFeedback(null);
      setRecoveryNotice(null);
      window.dispatchEvent(new CustomEvent("jonpc:authenticated"));
      setPassword("");
      closeAccountPanel();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to complete this request.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handlePasswordResetRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const response = await requestPasswordReset(email);
      setRecoveryNotice(response.message);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to request a reset link.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handlePasswordReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!initialPasswordResetToken) return;
    setError(null);
    setIsSubmitting(true);
    try {
      const response = await resetPassword(initialPasswordResetToken, password, confirmPassword);
      window.localStorage.removeItem(authTokenKey);
      setUser(null);
      setSavedBuilds([]);
      setOrders([]);
      onAuthChange?.(null);
      setPassword("");
      setConfirmPassword("");
      setMode("login");
      setRecoveryNotice(response.message);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to reset this password.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function logout() {
    window.localStorage.removeItem(authTokenKey);
    setUser(null);
    setSavedBuilds([]);
    setOrders([]);
    setOrdersError(null);
    setVerificationFeedback(null);
    onAuthChange?.(null);
    setIsAccountOpen(false);
    setIsBuildsOpen(false);
    setIsOrdersOpen(false);
    setExpandedOrderId(null);
  }

  async function resendVerification() {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    setVerificationBusy(true);
    try {
      const response = await sendEmailVerification(token);
      setVerificationFeedback({
        message: import.meta.env.DEV && response.message === "Verification email sent."
          ? "Link ready — check backend console."
          : response.message,
      });
    } catch (requestError) {
      setVerificationFeedback({
        message: requestError instanceof Error ? requestError.message : "Unable to send link.",
        error: true,
      });
    } finally {
      setVerificationBusy(false);
    }
  }

  return (
    <>
      <div className={isAccountOpen ? "account-entry account-menu-open" : "account-entry"} ref={accountEntryRef}>
        {user ? (
        <button className="account-button account-button-signed-in" type="button" onClick={() => setIsAccountOpen((open) => !open)} title="Open account" aria-expanded={isAccountOpen} aria-haspopup="menu" aria-label={`Open account for ${user.displayName}`}>
          <span className="account-status-dot" aria-hidden="true" />
          <span className="account-avatar" aria-hidden="true"><i /></span>
          <span className="account-user-name">{user.displayName}</span>
        </button>
        ) : (
        <button className="account-button account-button-icon" type="button" onClick={() => openPanel()} title="Account" aria-label="Open account">
          <span className="account-avatar" aria-hidden="true"><i /></span>
        </button>
        )}
        {user && (
          <div className="account-menu-popover" role="menu" aria-label="Account actions">
            <span className="account-menu-label">{user.email}</span>
            <span className={user.emailVerified ? "account-email-status account-email-verified" : "account-email-status"}>
              {user.emailVerified ? "Email verified" : "Email verification required"}
            </span>
            {verificationFeedback && (
              <span className={verificationFeedback.error ? "account-verification-message account-verification-error" : "account-verification-message"} role="status">
                {verificationFeedback.message}
              </span>
            )}
            {!user.emailVerified && (
              <button type="button" role="menuitem" onClick={resendVerification} disabled={verificationBusy}>
                {verificationBusy ? "Sending..." : "Send verification link"}<span aria-hidden="true">→</span>
              </button>
            )}
            {user.role === "ADMIN" && (
              <button type="button" role="menuitem" onClick={() => { window.location.hash = "/admin"; }}>Admin workspace <span aria-hidden="true">→</span></button>
            )}
            <button type="button" role="menuitem" onClick={logout}>Sign out <span aria-hidden="true">↗</span></button>
          </div>
        )}
      </div>
      {user && <div className="my-builds-entry"><button className="my-builds-nav-button" type="button" onClick={openBuildsPanel}><span className="my-builds-icon" aria-hidden="true" />My builds</button></div>}
      {user && <div className="order-history-entry"><button className="order-history-nav-button" type="button" onClick={() => { openOrdersPanel(); window.dispatchEvent(new CustomEvent("jonpc:order-requested")); }}><span className="order-history-icon" aria-hidden="true" />My orders</button></div>}

      {isOpen && (
        <div className="auth-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeAccountPanel(); }}>
          <section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-modal-title">
            <button className="auth-modal-close" type="button" onClick={closeAccountPanel} aria-label="Close account panel">×</button>
            <span className="section-kicker">JON. PC / Account</span>
            <h2 id="auth-modal-title">
              {mode === "login" && "Welcome back."}
              {mode === "register" && "Create your account."}
              {mode === "forgot" && (recoveryNotice ? "Check your email." : "Reset your password.")}
              {mode === "reset" && "Choose a new password."}
            </h2>
            {(mode === "login" || mode === "register" || (mode === "forgot" && recoveryNotice)) && (
              <p className="auth-modal-intro">
                {mode === "login" && "Sign in to keep your build requests close."}
                {mode === "register" && "Save your build requests and keep your next system close."}
                {mode === "forgot" && recoveryNotice}
              </p>
            )}

            {(mode === "login" || mode === "register") && (
              <div className="auth-mode-switch" role="tablist" aria-label="Account mode">
                <button className={mode === "login" ? "auth-mode-active" : ""} type="button" role="tab" aria-selected={mode === "login"} onClick={() => switchMode("login")}>Log in</button>
                <button className={mode === "register" ? "auth-mode-active" : ""} type="button" role="tab" aria-selected={mode === "register"} onClick={() => switchMode("register")}>Create account</button>
              </div>
            )}

            {(mode === "login" || mode === "register") && (
              <form className="auth-form" onSubmit={handleSubmit} autoComplete="off">
                {mode === "register" && (
                  <label><span>Display name</span><input value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="How should we address you?" autoComplete="off" required /></label>
                )}
                <label><span>Email</span><input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" autoComplete="email" required /></label>
                <label><span>Password <small>8 characters minimum</small></span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Your password" autoComplete={mode === "login" ? "current-password" : "new-password"} minLength={8} required /></label>
                {mode === "login" && <button className="auth-text-action" type="button" onClick={() => switchMode("forgot")}>Forgot password? <span aria-hidden="true">→</span></button>}
                {recoveryNotice && <p className="auth-success" role="status">{recoveryNotice}</p>}
                {error && <p className="auth-error" role="alert">{error}</p>}
                <button className="button button-primary auth-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? "Connecting..." : mode === "login" ? "Log in" : "Create account"}<span aria-hidden="true">↗</span></button>
              </form>
            )}

            {mode === "forgot" && (
              <form className="auth-form" onSubmit={handlePasswordResetRequest}>
                {!recoveryNotice && <label><span>Email</span><input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" autoComplete="email" required /></label>}
                {error && <p className="auth-error" role="alert">{error}</p>}
                {!recoveryNotice && <button className="button button-primary auth-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? "Preparing..." : "Send reset link"}<span aria-hidden="true">↗</span></button>}
                <button className="auth-back-action" type="button" onClick={() => switchMode("login")}>← Back to log in</button>
              </form>
            )}

            {mode === "reset" && (
              <form className="auth-form" onSubmit={handlePasswordReset}>
                <label><span>New password <small>8 characters minimum</small></span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="New password" autoComplete="new-password" minLength={8} required /></label>
                <label><span>Confirm password</span><input type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} placeholder="Repeat new password" autoComplete="new-password" minLength={8} required /></label>
                {error && <p className="auth-error" role="alert">{error}</p>}
                <button className="button button-primary auth-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? "Updating..." : "Update password"}<span aria-hidden="true">↗</span></button>
                <button className="auth-back-action" type="button" onClick={() => switchMode("login")}>← Back to log in</button>
              </form>
            )}
          </section>
        </div>
      )}

      {isBuildsOpen && user && (
        <div className="auth-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeBuildsPanel(); }}>
          <section className="auth-modal saved-builds-modal" role="dialog" aria-modal="true" aria-labelledby="saved-builds-title">
            <button className="auth-modal-close" type="button" onClick={closeBuildsPanel} aria-label="Close saved builds">×</button>
            <span className="section-kicker">JON. PC / Account</span>
            <h2 id="saved-builds-title">My builds.</h2>
            <p className="auth-modal-intro">Keep your configurations close and return to them when you are ready.</p>
            <div className="saved-builds-list">
              {buildsLoading && <p className="saved-builds-empty">Loading your builds...</p>}
              {!buildsLoading && savedBuilds.length === 0 && <p className="saved-builds-empty">No saved builds yet. Complete a configuration and save it here.</p>}
              {savedBuilds.map((build) => (
                <article className="saved-build-row" key={build.id}>
                  <div><strong>{build.name}</strong><span>{build.direction} / ${build.estimatedPrice.toLocaleString("en-AU")} AUD</span></div>
                  <div className="saved-build-row-actions">
                    <button type="button" onClick={() => { dismissBuildsPanel(); window.dispatchEvent(new CustomEvent("jonpc:load-build", { detail: build })); }}>Continue customising ↗</button>
                    <button type="button" onClick={() => { const token = window.localStorage.getItem(authTokenKey); if (!token) return; deleteSavedBuild(token, build.id).then(() => setSavedBuilds((current) => current.filter((item) => item.id !== build.id))).catch(() => undefined); }}>Delete ×</button>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </div>
      )}

      {isOrdersOpen && user && (
        <div className="auth-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeOrdersPanel(); }}>
          <section className="auth-modal order-history-modal" role="dialog" aria-modal="true" aria-labelledby="order-history-title">
            <button className="auth-modal-close" type="button" onClick={() => { closeOrdersPanel(); setExpandedOrderId(null); }} aria-label="Close order history">×</button>
            <span className="section-kicker">JON. PC / Orders</span>
            <h2 id="order-history-title">My orders.</h2>
            <div className="order-history-list">
              {ordersLoading && <p className="saved-builds-empty">Loading your orders...</p>}
              {!ordersLoading && ordersError && <p className="saved-builds-empty auth-error" role="alert">{ordersError}</p>}
              {!ordersLoading && !ordersError && orders.length === 0 && <p className="saved-builds-empty">No orders yet. Your submitted configurations will appear here.</p>}
              {orders.map((order) => (
                <article className="order-history-row" key={order.id}>
                  <div className="order-history-row-heading">
                    <div><strong>{order.orderReference ?? order.requestReference}</strong><span>{order.direction} / {formatOrderDate(order.createdAt)}</span></div>
                    <div className="order-history-price"><b>{order.quoteTotalCents != null ? `$${(order.quoteTotalCents / 100).toLocaleString("en-AU")} AUD incl. GST` : order.configuration.answers?.systemSku ? `$${order.estimatedPrice.toLocaleString("en-AU")} AUD incl. GST` : `$${order.estimatedPrice.toLocaleString("en-AU")} AUD estimated`}</b>{order.reward && <span>{formatRewardMoney(order.reward.cashbackAmountCents)} cashback · {rewardStatusLabel(order.reward.status)}</span>}</div>
                  </div>
                  <div className="order-history-row-footer">
                    {(!paymentDemoEnabled || customerOrderState(order).label !== "Ready to checkout") && <span className={`order-status order-status-${customerOrderState(order).tone}`}>{customerOrderState(order).label}</span>}
                    <div className="order-history-row-actions">
                      <button className="order-details-action" type="button" aria-expanded={expandedOrderId === order.id} onClick={() => setExpandedOrderId((current) => current === order.id ? null : order.id)}>{expandedOrderId === order.id ? "Hide details" : "View details"}</button>
                      {(!order.salesOrderId || (paymentDemoEnabled && ["CREATED", "FAILED"].includes(order.paymentStatus ?? ""))) && <CancelRequestAction order={order} />}
                      {order.invoiceId && order.invoiceNumber && <InvoiceDownloadButton invoiceId={order.invoiceId} invoiceNumber={order.invoiceNumber} compact />}
                      <OrderPrimaryAction order={order} onClose={dismissOrdersPanel} />
                    </div>
                  </div>
                  {expandedOrderId === order.id && <OrderDetails order={order} />}
                </article>
              ))}
            </div>
          </section>
        </div>
      )}
    </>
  );
}

function formatOrderDate(value: string) {
  return new Date(value).toLocaleDateString("en-AU", {
    timeZone: "Australia/Melbourne",
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function customerOrderState(order: OrderHistoryItem): { label: string; tone: string } {
  if (order.orderStatus === "REFUNDED") return { label: "Refunded", tone: "neutral" };
  if (order.orderStatus === "CANCELLED" || order.paymentStatus === "CANCELLED") return { label: "Cancelled", tone: "neutral" };
  if (order.paymentStatus === "SUCCEEDED" || order.orderStatus === "PAID" || order.orderStatus === "REWARD_ELIGIBLE") return { label: "Paid", tone: "success" };
  if (order.paymentStatus === "FAILED") return { label: "Payment failed", tone: "failed" };
  if (order.paymentStatus === "PROCESSING") return { label: "Payment processing", tone: "processing" };
  if (order.paymentStatus === "CREATED") return { label: "Awaiting payment", tone: "pending" };
  if (!order.configuration.answers?.systemSku && !order.salesOrderId) {
    if (order.quoteStatus === "ACCEPTED") return { label: "Quote accepted", tone: "success" };
    if (order.quoteStatus === "SENT" && order.quoteValidUntil && new Date(order.quoteValidUntil).getTime() < Date.now()) return { label: "Quote expired", tone: "neutral" };
    if (order.quoteStatus === "SENT") return { label: "Quote ready", tone: "ready" };
    return { label: "Under review", tone: "pending" };
  }
  if (order.salesOrderId) return { label: "Ready to checkout", tone: "ready" };
  return paymentDemoEnabled
    ? { label: "Ready to checkout", tone: "ready" }
    : { label: "Request received", tone: "pending" };
}

function getOptionLabel(options: Array<{ id: string; label: string }>, id: string) {
  return options.find((option) => option.id === id)?.label ?? id;
}

function OrderPrimaryAction({ order, onClose }: { order: OrderHistoryItem; onClose: () => void }) {
  const [accepting, setAccepting] = useState(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [openedAt] = useState(Date.now);
  const openCheckout = () => {
    onClose();
    window.dispatchEvent(new CustomEvent("jonpc:open-checkout", { detail: { requestReference: order.requestReference } }));
  };

  if (order.paymentStatus === "SUCCEEDED" || order.orderStatus === "PAID" || order.orderStatus === "REWARD_ELIGIBLE") {
    return <button className="order-primary-action" type="button" onClick={() => { onClose(); window.dispatchEvent(new CustomEvent("jonpc:open-rewards")); }}>View rewards →</button>;
  }
  if (order.paymentStatus === "FAILED") {
    return <button className="order-primary-action" type="button" onClick={openCheckout}>Retry payment →</button>;
  }
  if (order.paymentStatus === "CREATED") {
    return <button className="order-primary-action" type="button" onClick={openCheckout}>Continue payment →</button>;
  }
  if (!order.configuration.answers?.systemSku) {
    if (order.quoteStatus === "SENT" && order.quoteValidUntil && new Date(order.quoteValidUntil).getTime() < openedAt) return null;
    if (order.quoteStatus === "SENT" && order.quoteId) return <><button className="order-primary-action" type="button" disabled={accepting} onClick={async () => {
      const token = window.localStorage.getItem(authTokenKey);
      if (!token) return;
      setAccepting(true);
      setQuoteError(null);
      try {
        await acceptCustomQuote(token, order.quoteId!);
        window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
        if (paymentDemoEnabled) openCheckout();
      } catch (error) { setQuoteError(error instanceof Error ? error.message : "Unable to accept quote."); }
      finally { setAccepting(false); }
    }}>{accepting ? "Confirming..." : "Accept final quote →"}</button>{quoteError && <small className="auth-error" role="alert">{quoteError}</small>}</>;
    if (order.quoteStatus === "ACCEPTED" && paymentDemoEnabled) return <button className="order-primary-action" type="button" onClick={openCheckout}>Continue to checkout →</button>;
    return null;
  }
  if (paymentDemoEnabled && !["PROCESSING", "CANCELLED"].includes(order.paymentStatus ?? "") && !["CANCELLED", "REFUNDED"].includes(order.orderStatus ?? "")) {
    return <button className="order-primary-action" type="button" onClick={openCheckout}>Continue to checkout →</button>;
  }
  if (!order.salesOrderId) return <span className="order-next-step">Awaiting final quote</span>;
  return null;
}

function OrderDetails({ order }: { order: OrderHistoryItem }) {
  const systemSku = order.configuration.answers?.systemSku;
  const configuration = systemSku ? [
    ["System", order.configuration.answers.systemName],
    ["SKU", systemSku],
    ...["CPU", "GPU", "Memory", "Storage", "Motherboard", "Cooler", "PSU", "Case", "Fans", "Networking", "OS"].filter((key) => order.configuration.answers[key]).map((key) => [key, order.configuration.answers[key]]),
  ] : [
    ["CPU", getOptionLabel(cpuOptions, order.configuration.cpuId)],
    ["GPU", getOptionLabel(gpuOptions, order.configuration.gpuId)],
    ["Memory", getOptionLabel(memoryOptions, order.configuration.memoryId)],
    ["Storage", getOptionLabel(storageOptions, order.configuration.storageId)],
    ["Motherboard", getOptionLabel(motherboardOptions, order.configuration.motherboardId)],
    ["Power supply", getOptionLabel(psuOptions, order.configuration.psuId)],
    ["Case", getOptionLabel(caseOptions, order.configuration.caseId)],
    ["Cooling", getOptionLabel(coolingOptions, order.configuration.coolingId)],
  ];

  return (
    <div className="order-history-details-panel">
      <div className="order-history-references">
        {order.orderReference && <div><span>Order</span><strong>{order.orderReference}</strong></div>}
        <div><span>Build request</span><strong>{order.requestReference}</strong></div>
        {order.paymentReference && <div><span>Payment</span><strong>{order.paymentReference}</strong></div>}
        {order.invoiceId && order.invoiceNumber && <div><span>Tax invoice</span><InvoiceDownloadButton invoiceId={order.invoiceId} invoiceNumber={order.invoiceNumber} /></div>}
        {order.quoteTotalCents != null && <div><span>Final quote</span><strong>${(order.quoteTotalCents / 100).toLocaleString("en-AU")} incl. GST{order.quoteValidUntil ? ` · Valid until ${formatOrderDate(order.quoteValidUntil)}` : ""}</strong></div>}
      </div>
      {order.quoteNote && <p className="order-next-step">{order.quoteNote}</p>}
      {order.reward && <div className="order-founder-summary">
        <div><span>Founder reward</span><strong>#{String(order.reward.founderNumber).padStart(2, "0")} · {order.reward.tierName}</strong></div>
        <div><span>Cashback</span><strong>{formatRewardMoney(order.reward.cashbackAmountCents)}</strong></div>
        <div><span>Status</span><strong>{rewardStatusLabel(order.reward.status)}</strong></div>
        <p>{rewardTiming(order.reward)}</p>
      </div>}
      <div className="order-history-configuration">
        {configuration.map(([label, value]) => <div key={label}><span>{label}</span><strong>{value}</strong></div>)}
      </div>
    </div>
  );
}

function formatRewardMoney(cents: number) {
  return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cents / 100);
}

function rewardStatusLabel(status: string) {
  return ({ LOCKED: "Locked", PAYABLE: "Ready for payout", PROCESSING: "Processing", PAID: "Paid", FAILED: "Needs attention", VOID: "Cancelled" } as Record<string, string>)[status] ?? status;
}

function rewardTiming(reward: NonNullable<OrderHistoryItem["reward"]>) {
  if (reward.status === "PAID") return `Returned to the original payment method${reward.paidAt ? ` on ${formatOrderDate(reward.paidAt)}` : ""}${reward.payoutReference ? ` · ${reward.payoutReference}` : ""}.`;
  if (reward.payoutDueAt) return `Payout scheduled by ${formatOrderDate(reward.payoutDueAt)} to the original payment method.`;
  if (reward.status === "FAILED") return "Payout needs attention. JON.PC support will review it.";
  return "JON.PC will initiate eligible cashback within 30 days after confirmed delivery.";
}

function CancelRequestAction({ order }: { order: OrderHistoryItem }) {
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const label = order.salesOrderId ? "Cancel order" : "Cancel request";

  async function cancel() {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    setBusy(true);
    setError(null);
    try {
      await cancelOrderRequest(token, order.id);
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to cancel.");
      setConfirming(false);
    } finally { setBusy(false); }
  }

  return <div className="order-cancel-action">
    {confirming ? <><span>Cancel this {order.salesOrderId ? "order" : "request"}?</span><button type="button" onClick={() => void cancel()} disabled={busy}>{busy ? "Cancelling..." : "Yes, cancel"}</button><button type="button" onClick={() => setConfirming(false)} disabled={busy}>Keep it</button></> : <button type="button" onClick={() => setConfirming(true)}>{label}</button>}
    {error && <small role="alert">{error}</small>}
  </div>;
}

function InvoiceDownloadButton({ invoiceId, invoiceNumber, compact = false }: { invoiceId: string; invoiceNumber: string; compact?: boolean }) {
  const [downloading, setDownloading] = useState(false);

  async function download() {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    setDownloading(true);
    try {
      await downloadInvoicePdf(token, invoiceId, invoiceNumber);
    } finally {
      setDownloading(false);
    }
  }

  const label = compact ? "Invoice ↓" : `${invoiceNumber} ↓`;
  return <button className={`order-invoice-download${compact ? " order-invoice-download-compact" : ""}`} type="button" onClick={download} disabled={downloading}>{downloading ? "Preparing..." : label}</button>;
}

export default AuthPanel;
