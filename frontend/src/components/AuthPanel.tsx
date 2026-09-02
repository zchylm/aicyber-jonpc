import { useEffect, useRef, useState, type FormEvent } from "react";
import { authTokenKey, fetchCurrentUser, login, register, type AuthUser } from "../api/auth";
import { deleteSavedBuild, fetchOrderHistory, fetchSavedBuilds, type OrderHistoryItem, type SavedBuild } from "../api/configurator";
import { cpuOptions, gpuOptions } from "../data/performance";
import { memoryOptions } from "../data/memory";
import { storageOptions } from "../data/storage";

type AuthMode = "login" | "register";

type AuthPanelProps = {
  onAuthChange?: (user: AuthUser | null) => void;
};

function AuthPanel({ onAuthChange }: AuthPanelProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [isAccountOpen, setIsAccountOpen] = useState(false);
  const [isBuildsOpen, setIsBuildsOpen] = useState(false);
  const [isOrdersOpen, setIsOrdersOpen] = useState(false);
  const [savedBuilds, setSavedBuilds] = useState<SavedBuild[]>([]);
  const [orders, setOrders] = useState<OrderHistoryItem[]>([]);
  const [buildsLoading, setBuildsLoading] = useState(false);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState<string | null>(null);
  const [mode, setMode] = useState<AuthMode>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const accountEntryRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    fetchCurrentUser(token)
      .then((currentUser) => {
        setUser(currentUser);
        onAuthChange?.(currentUser);
      })
      .catch(() => {
        window.localStorage.removeItem(authTokenKey);
        onAuthChange?.(null);
      });
  }, [onAuthChange]);

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
      if (event.key === "Escape") setIsOpen(false);
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen]);

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

  function openPanel(nextMode: AuthMode = "login") {
    setMode(nextMode);
    setError(null);
    setIsOpen(true);
  }

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setError(null);
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
      setPassword("");
      setIsOpen(false);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to complete this request.");
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
    onAuthChange?.(null);
    setIsAccountOpen(false);
    setIsBuildsOpen(false);
    setIsOrdersOpen(false);
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
            <button type="button" role="menuitem" onClick={logout}>Sign out <span aria-hidden="true">↗</span></button>
          </div>
        )}
      </div>
      {user && <div className="my-builds-entry"><button className="my-builds-nav-button" type="button" onClick={() => setIsBuildsOpen(true)}><span className="my-builds-icon" aria-hidden="true" />My builds</button></div>}
      {user && <div className="order-history-entry"><button className="order-history-nav-button" type="button" onClick={() => setIsOrdersOpen(true)}><span className="order-history-icon" aria-hidden="true" />Order history</button></div>}

      {isOpen && (
        <div className="auth-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setIsOpen(false); }}>
          <section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-modal-title">
            <button className="auth-modal-close" type="button" onClick={() => setIsOpen(false)} aria-label="Close account panel">×</button>
            <span className="section-kicker">JON. PC / Account</span>
            <h2 id="auth-modal-title">{mode === "login" ? "Welcome back." : "Create your account."}</h2>
            <p className="auth-modal-intro">{mode === "login" ? "Sign in to keep your build requests close." : "Save your build requests and keep your next system close."}</p>

            <div className="auth-mode-switch" role="tablist" aria-label="Account mode">
              <button className={mode === "login" ? "auth-mode-active" : ""} type="button" role="tab" aria-selected={mode === "login"} onClick={() => switchMode("login")}>Log in</button>
              <button className={mode === "register" ? "auth-mode-active" : ""} type="button" role="tab" aria-selected={mode === "register"} onClick={() => switchMode("register")}>Create account</button>
            </div>

            <form className="auth-form" onSubmit={handleSubmit} autoComplete="off">
              {mode === "register" && (
                <label><span>Display name</span><input value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="How should we address you?" autoComplete="off" required /></label>
              )}
              <label><span>Email</span><input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" autoComplete="off" required /></label>
              <label><span>Password <small>8 characters minimum</small></span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Your password" autoComplete="new-password" minLength={8} required /></label>
              {error && <p className="auth-error" role="alert">{error}</p>}
              <button className="button button-primary auth-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? "Connecting..." : mode === "login" ? "Log in" : "Create account"}<span aria-hidden="true">↗</span></button>
            </form>
            <p className="auth-note">Your password is encrypted before it is stored. JON. PC never displays it.</p>
          </section>
        </div>
      )}

      {isBuildsOpen && user && (
        <div className="auth-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setIsBuildsOpen(false); }}>
          <section className="auth-modal saved-builds-modal" role="dialog" aria-modal="true" aria-labelledby="saved-builds-title">
            <button className="auth-modal-close" type="button" onClick={() => setIsBuildsOpen(false)} aria-label="Close saved builds">×</button>
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
                    <button type="button" onClick={() => { setIsBuildsOpen(false); window.dispatchEvent(new CustomEvent("jonpc:load-build", { detail: build })); }}>Continue customising ↗</button>
                    <button type="button" onClick={() => { const token = window.localStorage.getItem(authTokenKey); if (!token) return; deleteSavedBuild(token, build.id).then(() => setSavedBuilds((current) => current.filter((item) => item.id !== build.id))).catch(() => undefined); }}>Delete ×</button>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </div>
      )}

      {isOrdersOpen && user && (
        <div className="auth-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setIsOrdersOpen(false); }}>
          <section className="auth-modal order-history-modal" role="dialog" aria-modal="true" aria-labelledby="order-history-title">
            <button className="auth-modal-close" type="button" onClick={() => setIsOrdersOpen(false)} aria-label="Close order history">×</button>
            <span className="section-kicker">JON. PC / Orders</span>
            <h2 id="order-history-title">Order history.</h2>
            <p className="auth-modal-intro">Track the configurations you have asked JON. PC to review.</p>
            <div className="order-history-list">
              {ordersLoading && <p className="saved-builds-empty">Loading your orders...</p>}
              {!ordersLoading && ordersError && <p className="saved-builds-empty auth-error" role="alert">{ordersError}</p>}
              {!ordersLoading && !ordersError && orders.length === 0 && <p className="saved-builds-empty">No build requests yet. Your submitted configurations will appear here.</p>}
              {orders.map((order) => (
                <article className="order-history-row" key={order.id}>
                  <div className="order-history-row-heading">
                    <div><strong>{order.requestReference}</strong><span>{order.direction} / {formatOrderDate(order.createdAt)}</span></div>
                    <b>${order.estimatedPrice.toLocaleString("en-AU")} AUD</b>
                  </div>
                  <div className="order-history-row-details">
                    <span>{getOptionLabel(cpuOptions, order.configuration.cpuId)}</span>
                    <span>{getOptionLabel(gpuOptions, order.configuration.gpuId)}</span>
                    <span>{getOptionLabel(memoryOptions, order.configuration.memoryId)}</span>
                    <span>{getOptionLabel(storageOptions, order.configuration.storageId)}</span>
                  </div>
                  <span className="order-status">{formatOrderStatus(order.status)}</span>
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

function formatOrderStatus(value: string) {
  return value.toLowerCase().replaceAll("_", " ");
}

function getOptionLabel(options: Array<{ id: string; label: string }>, id: string) {
  return options.find((option) => option.id === id)?.label ?? id;
}

export default AuthPanel;
