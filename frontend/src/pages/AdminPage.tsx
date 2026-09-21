import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  downloadAdminInvoice,
  fetchAdminOrder,
  fetchAdminOrders,
  fetchAdminOverview,
  fetchAdminRewards,
  resendAdminInvoice,
  type AdminOrder,
  type AdminOrderDetail,
  type AdminOverview,
  type AdminRewards,
} from "../api/admin";
import { authTokenKey, fetchCurrentUser, login, type AuthUser } from "../api/auth";
import { fetchConfiguratorCatalog, type ConfiguratorCatalog } from "../api/configurator";
import BrandLockup from "../components/BrandLockup";
import AdminInventoryWorkspace from "./AdminInventoryWorkspace";
import AdminCustomBuildReview from "./AdminCustomBuildReview";
import "./AdminPage.css";

type AdminView = "overview" | "orders" | "custom-build-review" | "inventory" | "rewards";
type RewardView = "commitments" | "failed";

function AdminPage() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [authReady, setAuthReady] = useState(() => !window.localStorage.getItem(authTokenKey));
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [authError, setAuthError] = useState<string | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [view, setView] = useState<AdminView>("overview");
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [orderTotal, setOrderTotal] = useState(0);
  const [orderQuery, setOrderQuery] = useState("");
  const [orderStatus, setOrderStatus] = useState("");
  const [selectedOrder, setSelectedOrder] = useState<AdminOrderDetail | null>(null);
  const [rewards, setRewards] = useState<AdminRewards | null>(null);
  const [rewardView, setRewardView] = useState<RewardView>("commitments");
  const [inventoryRefreshKey, setInventoryRefreshKey] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) return;
    fetchCurrentUser(token)
      .then(setUser)
      .catch(() => window.localStorage.removeItem(authTokenKey))
      .finally(() => setAuthReady(true));
  }, []);

  const loadAdminData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [nextOverview, nextRewards] = await Promise.all([
        fetchAdminOverview(), fetchAdminRewards(),
      ]);
      setOverview(nextOverview);
      setRewards(nextRewards);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load admin data");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const nextOrders = await fetchAdminOrders(orderQuery, orderStatus);
      setOrders(nextOrders.items);
      setOrderTotal(nextOrders.total);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load orders");
    } finally {
      setLoading(false);
    }
  }, [orderQuery, orderStatus]);

  useEffect(() => {
    if (user?.role !== "ADMIN" || view !== "overview") return;
    const loadId = window.setTimeout(() => void loadAdminData(), 0);
    return () => window.clearTimeout(loadId);
  }, [loadAdminData, user?.role, view]);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    const loadId = window.setTimeout(() => void loadOrders(), 250);
    return () => window.clearTimeout(loadId);
  }, [loadOrders, user?.role]);

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthBusy(true);
    setAuthError(null);
    try {
      const response = await login({ email, password });
      window.localStorage.setItem(authTokenKey, response.accessToken);
      setUser(response.user);
      setPassword("");
    } catch (requestError) {
      setAuthError(requestError instanceof Error ? requestError.message : "Unable to sign in");
    } finally {
      setAuthBusy(false);
    }
  }

  async function openOrder(orderId: string) {
    setLoading(true);
    setError(null);
    try {
      setSelectedOrder(await fetchAdminOrder(orderId));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to load order");
    } finally {
      setLoading(false);
    }
  }

  function signOut() {
    window.localStorage.removeItem(authTokenKey);
    setUser(null);
  }

  async function refreshAdminData() {
    await Promise.all([loadAdminData(), loadOrders()]);
    setInventoryRefreshKey((current) => current + 1);
  }

  if (!authReady) return <div className="admin-centred"><span className="admin-loader" />Loading admin workspace</div>;
  if (!user) return <AdminLogin email={email} password={password} busy={authBusy} error={authError} onEmail={setEmail} onPassword={setPassword} onSubmit={handleLogin} />;
  if (user.role !== "ADMIN") return (
    <div className="admin-access-page">
      <BrandLockup />
      <div><span>JON. PC / Admin</span><h1>Access restricted.</h1><p>This workspace is available to authorised administrators.</p><a href="/#top">Return to JON. PC</a><button type="button" onClick={signOut}>Use another account</button></div>
    </div>
  );

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <a href="/#top" className="admin-brand" aria-label="Return to JON. PC"><BrandLockup /></a>
        <span className="admin-workspace-label">Admin workspace</span>
        <nav aria-label="Admin navigation">
          {(["overview", "orders", "custom-build-review", "inventory", "rewards"] as AdminView[]).map((item) => (
            <button className={view === item ? "active" : ""} type="button" onClick={() => setView(item)} key={item}>
              <i />{title(item)}
              {item === "orders" && overview && overview.pendingPaymentOrders > 0 && <b>{overview.pendingPaymentOrders}</b>}
              {item === "rewards" && overview && overview.failedRewardEvents > 0 && <b className="alert">{overview.failedRewardEvents}</b>}
            </button>
          ))}
        </nav>
        <div className="admin-profile"><span>{initials(user.displayName)}</span><div><strong>{user.displayName}</strong><small>{user.email}</small></div><button type="button" onClick={signOut}>Sign out</button></div>
      </aside>

      <main className="admin-main">
        <header className="admin-topbar"><div><span>JON. PC operations</span><h1>{title(view)}</h1></div><button className="admin-refresh" type="button" onClick={() => void refreshAdminData()} disabled={loading}>{loading ? "Refreshing…" : "Refresh data"}</button></header>
        {error && <div className="admin-error" role="alert">{error}</div>}

        {view === "overview" && <Overview overview={overview} onNavigate={setView} />}
        {view === "orders" && <Orders orders={orders} total={orderTotal} query={orderQuery} status={orderStatus} selected={selectedOrder} onQuery={setOrderQuery} onStatus={setOrderStatus} onOpen={openOrder} onClose={() => setSelectedOrder(null)} />}
        {view === "custom-build-review" && <AdminCustomBuildReview refreshKey={inventoryRefreshKey} />}
        {view === "inventory" && <AdminInventoryWorkspace refreshKey={inventoryRefreshKey} />}
        {view === "rewards" && <Rewards rewards={rewards} view={rewardView} onView={setRewardView} />}
      </main>
    </div>
  );
}

function AdminLogin({ email, password, busy, error, onEmail, onPassword, onSubmit }: { email: string; password: string; busy: boolean; error: string | null; onEmail: (value: string) => void; onPassword: (value: string) => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="admin-login-page"><section><BrandLockup /><span className="admin-login-kicker">Admin workspace</span><h1>Operations, clearly.</h1><form onSubmit={onSubmit}><label>Email<input type="email" value={email} onChange={(event) => onEmail(event.target.value)} autoComplete="email" required /></label><label>Password<input type="password" value={password} onChange={(event) => onPassword(event.target.value)} autoComplete="current-password" required /></label>{error && <p role="alert">{error}</p>}<button type="submit" disabled={busy}>{busy ? "Signing in…" : "Sign in to admin"}<span>→</span></button></form><a href="/#top">← Return to JON. PC</a></section><div className="admin-login-visual"><span>JON. PC</span><strong>ADMIN</strong><small>ORDERS / INVENTORY / REWARDS</small></div></div>;
}

function Overview({ overview, onNavigate }: { overview: AdminOverview | null; onNavigate: (view: AdminView) => void }) {
  if (!overview) return <EmptyState label="Loading overview" />;
  const orderIssues = overview.paymentIssues + overview.invoiceEmailIssues;
  const actions: Array<{ label: string; value: number; detail: string; target: AdminView; link: string }> = [];
  if (orderIssues) actions.push({ label: "Orders need attention", value: orderIssues, detail: `${overview.paymentIssues} payment · ${overview.invoiceEmailIssues} invoice email`, target: "orders", link: "Review orders" });
  if (overview.customReviewsNeeded) actions.push({ label: "Builds waiting for review", value: overview.customReviewsNeeded, detail: overview.oldestCustomReviewHours ? `Oldest waiting ${duration(overview.oldestCustomReviewHours)}` : "New custom requests", target: "custom-build-review", link: "Open review" });
  if (overview.inventoryAlerts.length) actions.push({ label: "Inventory needs attention", value: overview.inventoryAlerts.length, detail: overview.inventoryAlerts[0] ? `${overview.inventoryAlerts[0].name} needs attention` : "Stock is at or below reorder level", target: "inventory", link: "Open inventory" });
  if (overview.failedRewardEvents) actions.push({ label: "Reward exceptions", value: overview.failedRewardEvents, detail: "Failed events need investigation", target: "rewards", link: "Investigate" });
  const today = [
    ["Orders", String(overview.ordersToday)], ["Sales", money(overview.revenueTodayCents)],
    ["Cashback committed", money(overview.rewardsAllocatedTodayCents)],
    ["Custom builds", String(overview.customRequestsToday)],
  ];
  return <><section className="admin-section-heading admin-overview-heading"><span>JON. PC / Operations</span><h2>{greeting()}.</h2><p>Here’s what needs your attention.</p></section>
    <section className="admin-overview-block"><div className="admin-overview-title"><span>Action required</span><small>{actions.length ? `${actions.length} areas need attention` : "No outstanding exceptions"}</small></div>{actions.length ? <div className="admin-action-grid">{actions.map((action) => <button type="button" onClick={() => onNavigate(action.target)} key={action.label}><strong>{action.value}</strong><div><span>{action.label}</span><small>{action.detail}</small><b>{action.link} →</b></div></button>)}</div> : <div className="admin-all-clear"><i />You’re all caught up.<small>New exceptions and reviews will appear here.</small></div>}</section>
    <section className="admin-overview-block"><div className="admin-overview-title"><span>Today</span><small>Melbourne time</small></div><div className="admin-today-grid">{today.map(([label, value]) => <div key={label}><span>{label}</span><strong>{value}</strong></div>)}</div></section>
    <div className="admin-overview-lower"><section className="admin-overview-block"><div className="admin-overview-title"><span>Recent activity</span><small>Latest across JON. PC</small></div>{overview.recentActivity.length ? <ol className="admin-activity-feed">{overview.recentActivity.map((item, index) => <li key={`${item.occurredAt}-${index}`}><time>{activityTime(item.occurredAt)}</time><i className={item.type.toLowerCase()} /><div><strong>{item.title}</strong><small>{item.detail}</small></div>{item.amountCents != null && <b>{money(item.amountCents)}</b>}</li>)}</ol> : <div className="admin-overview-empty">Activity will appear as orders and requests arrive.</div>}</section>
      <section className="admin-overview-block"><div className="admin-overview-title"><span>Inventory watch</span><small>{overview.inventoryAlerts.length ? "Low stock and sold out" : "No current alerts"}</small></div>{overview.inventoryAlerts.length ? <div className="admin-inventory-watch">{overview.inventoryAlerts.map((item) => <button type="button" onClick={() => onNavigate("inventory")} key={`${item.itemType}-${item.sku}`}><div><strong>{item.name}</strong><small>{item.itemType === "SYSTEM" ? "Whole system" : item.sku}</small></div><span>{item.availableQuantity === 0 ? "Sold out" : item.availableQuantity}<small>{item.availableQuantity === 0 ? item.sku : "available"}</small></span></button>)}</div> : <div className="admin-overview-empty">Stock alerts will appear here.</div>}</section></div></>;
}

function greeting() {
  const hour = Number(new Intl.DateTimeFormat("en-AU", { timeZone: "Australia/Melbourne", hour: "2-digit", hour12: false }).format(new Date()));
  return hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
}
function duration(hours: number) { return hours < 24 ? `${hours}h` : `${Math.floor(hours / 24)}d`; }
function activityTime(value: string) { return new Date(value).toLocaleTimeString("en-AU", { hour: "2-digit", minute: "2-digit" }); }

function Orders({ orders, total, query, status, selected, onQuery, onStatus, onOpen, onClose }: { orders: AdminOrder[]; total: number; query: string; status: string; selected: AdminOrderDetail | null; onQuery: (value: string) => void; onStatus: (value: string) => void; onOpen: (id: string) => void; onClose: () => void }) {
  return <><section className="admin-section-heading compact"><span>{total} orders</span><h2>Find any order.</h2></section><div className="admin-filters"><label><span>Search</span><input value={query} onChange={(event) => onQuery(event.target.value)} placeholder="Order, invoice, payment, customer ID or email" /></label><label><span>Status</span><select value={status} onChange={(event) => onStatus(event.target.value)}><option value="">All statuses</option><option>PENDING_PAYMENT</option><option>PAID</option><option>REWARD_ELIGIBLE</option><option>CANCELLED</option><option>REFUNDED</option></select></label></div><div className="admin-table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Total</th><th>Payment</th><th>Reward</th><th>Created</th></tr></thead><tbody>{orders.map((order) => <tr key={order.id} onClick={() => onOpen(order.id)}><td><strong>{order.orderReference}</strong><Status value={order.status} />{order.invoiceNumber && <small>{order.invoiceNumber}</small>}</td><td>{order.customerName}<small>{order.customerReference}</small><small>{order.customerEmail}</small></td><td>{money(order.amountCents)}</td><td><Status value={order.paymentStatus ?? "NOT STARTED"} /><small>{order.paymentReference}</small></td><td><Status value={order.rewardStatus ?? "NOT ELIGIBLE"} /></td><td>{date(order.createdAt)}<small>View details →</small></td></tr>)}</tbody></table>{orders.length === 0 && <EmptyState label="No matching orders" />}</div>{selected && <OrderDrawer order={selected} onClose={onClose} />}</>;
}

function OrderDrawer({ order, onClose }: { order: AdminOrderDetail; onClose: () => void }) {
  const [catalog, setCatalog] = useState<ConfiguratorCatalog | null>(null);
  const answers = order.configuration.answers && typeof order.configuration.answers === "object"
    ? order.configuration.answers as Record<string, unknown> : {};
  const featured = typeof answers.systemSku === "string";
  useEffect(() => {
    if (featured) return;
    fetchConfiguratorCatalog().then(setCatalog).catch(() => {});
  }, [featured]);
  const featuredSpecs = ["CPU", "GPU", "Memory", "Storage", "Motherboard", "PSU", "Case", "Cooler"];
  const customSpecs = [
    ["cpuId", "Processor"], ["gpuId", "Graphics"], ["memoryId", "Memory"], ["storageId", "Storage"],
    ["motherboardId", "Motherboard"], ["psuId", "Power supply"], ["caseId", "Case"], ["coolingId", "Cooling"],
  ] as const;
  return <div className="admin-drawer-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><aside className="admin-drawer" aria-label={`Order ${order.orderReference}`}><button className="admin-drawer-close" type="button" onClick={onClose}>×</button><span>Order detail</span><h2>{order.orderReference}</h2><div className="admin-detail-grid"><div><small>Customer</small><strong>{order.customerName}</strong><p>{order.customerReference}</p><p>{order.customerEmail}</p></div><div><small>Total incl. GST</small><strong>{money(order.amountCents)}</strong><Status value={order.status} /></div></div>
    <DetailSection title={featured ? "Featured system" : "Selected configuration"}><dl><div><dt>Build request</dt><dd>{order.buildRequestReference ?? "—"}</dd></div>{featured ? <><div><dt>System</dt><dd>{String(answers.systemName ?? answers.systemSku)}</dd></div><div><dt>SKU</dt><dd>{String(answers.systemSku)}</dd></div>{featuredSpecs.filter((key) => answers[key]).map((key) => <div key={key}><dt>{key}</dt><dd>{String(answers[key])}</dd></div>)}</> : <>{customSpecs.filter(([key]) => order.configuration[key]).map(([key, label]) => { const id = String(order.configuration[key]); const name = catalog?.options[key.replace(/Id$/, "")]?.find((option) => option.id === id)?.label ?? id; return <div key={key}><dt>{label}</dt><dd>{name}</dd></div>; })}{Object.entries(answers).filter(([, value]) => typeof value === "string" && value).map(([key, value]) => <div key={key}><dt>{formatLabel(key)}</dt><dd>{String(value)}</dd></div>)}{!order.buildRequestReference && <p className="admin-muted">No product configuration is attached to this order.</p>}</>}</dl></DetailSection>
    <DetailSection title="Delivery details">{order.delivery ? <div className="admin-delivery-card"><strong>{order.delivery.recipientName}</strong><a href={`tel:${order.delivery.phone}`}>{order.delivery.phone}</a><address>{order.delivery.addressLine1}{order.delivery.addressLine2 && <><br />{order.delivery.addressLine2}</>}<br />{order.delivery.suburb} {order.delivery.state} {order.delivery.postcode}<br />Australia</address></div> : <p className="admin-delivery-missing">No delivery address is recorded for this order. Do not dispatch until the details are confirmed.</p>}</DetailSection>
    <DetailSection title="Payments">{order.payments.length === 0 ? <p className="admin-muted">No payment attempts.</p> : order.payments.map((payment) => <article className="admin-payment-row" key={payment.id}><div><strong>{payment.paymentReference}</strong><small>{payment.provider} / {date(payment.createdAt)}</small></div><div><b>{money(payment.amountCents)}</b><Status value={payment.status} /></div></article>)}</DetailSection><DetailSection title="Tax invoice">{order.invoice ? <AdminInvoicePanel invoice={order.invoice} /> : <p className="admin-muted">No tax invoice issued.</p>}</DetailSection><DetailSection title="Founder reward">{order.reward ? <div className="admin-reward-detail"><strong>Founder #{String(order.reward.founderNumber).padStart(2, "0")} · {order.reward.tierName}</strong><Status value={order.reward.status} /><div><span>{order.reward.rateBasisPoints / 100}% rate</span><span>{money(order.reward.capCents)} cap</span><span>{money(order.reward.cashbackAmountCents)} committed</span></div></div> : <p className="admin-muted">No Founder commitment for this order.</p>}</DetailSection><DetailSection title="Timeline"><ol className="admin-timeline"><li><i /><div><strong>Order created</strong><span>{dateTime(order.createdAt)}</span></div></li>{order.paidAt && <li><i /><div><strong>Payment confirmed</strong><span>{dateTime(order.paidAt)}</span></div></li>}{order.invoice && <li><i /><div><strong>Tax invoice issued</strong><span>{dateTime(order.invoice.issuedAt)}</span></div></li>}{order.rewardEligibleAt && <li><i /><div><strong>Founder cashback locked</strong><span>{dateTime(order.rewardEligibleAt)}</span></div></li>}</ol></DetailSection></aside></div>;
}

function AdminInvoicePanel({ invoice }: { invoice: NonNullable<AdminOrderDetail["invoice"]> }) {
  const [busy, setBusy] = useState<"download" | "resend" | null>(null);
  const [deliveryStatus, setDeliveryStatus] = useState(invoice.deliveryStatus ?? "PENDING");
  const [actionError, setActionError] = useState<string | null>(null);

  async function download() {
    setBusy("download"); setActionError(null);
    try { await downloadAdminInvoice(invoice.id, invoice.invoiceNumber); }
    catch (requestError) { setActionError(requestError instanceof Error ? requestError.message : "Unable to download invoice"); }
    finally { setBusy(null); }
  }

  async function resend() {
    setBusy("resend"); setActionError(null);
    try { setDeliveryStatus((await resendAdminInvoice(invoice.id)).deliveryStatus); }
    catch (requestError) { setActionError(requestError instanceof Error ? requestError.message : "Unable to resend invoice"); }
    finally { setBusy(null); }
  }

  return <div className="admin-invoice-card"><div className="admin-invoice-heading"><div><strong>{invoice.invoiceNumber}</strong><small>{date(invoice.issuedAt)}</small></div><Status value={invoice.status} /></div><div className="admin-invoice-values"><span>Subtotal<strong>{moneyExact(invoice.subtotalExGstCents)}</strong></span><span>GST<strong>{moneyExact(invoice.gstCents)}</strong></span><span>Total<strong>{moneyExact(invoice.totalCents)}</strong></span></div><div className="admin-invoice-actions"><span>Email <Status value={deliveryStatus} /></span><button type="button" onClick={download} disabled={busy !== null}>{busy === "download" ? "Preparing…" : "Download PDF ↓"}</button><button type="button" onClick={resend} disabled={busy !== null}>{busy === "resend" ? "Sending…" : "Resend email →"}</button></div>{actionError && <p className="admin-inline-error" role="alert">{actionError}</p>}</div>;
}

function Rewards({ rewards, view, onView }: { rewards: AdminRewards | null; view: RewardView; onView: (view: RewardView) => void }) {
  if (!rewards) return <EmptyState label="Loading rewards" />;
  return <><section className="admin-section-heading compact"><span>Founder campaign</span><h2>Cashback commitments.</h2></section><div className="admin-kpi-grid"><article><span>Founder positions</span><strong>{rewards.campaign.confirmedPositions} / {rewards.campaign.maxPositions}</strong></article><article><span>Committed</span><strong>{money(rewards.campaign.committedCents)}</strong></article><article><span>Paid</span><strong>{money(rewards.campaign.paidCents)}</strong></article><article><span>Liability remaining</span><strong>{money(rewards.campaign.availableBudgetCents)}</strong></article></div><div className="admin-subnav">{(["commitments", "failed"] as RewardView[]).map((item) => <button className={view === item ? "active" : ""} type="button" onClick={() => onView(item)} key={item}>{title(item)}{item === "failed" && rewards.failedEvents.length > 0 && <b>{rewards.failedEvents.length}</b>}</button>)}</div><div className="admin-table-wrap"><RewardTable rewards={rewards} view={view} /></div></>;
}

function RewardTable({ rewards, view }: { rewards: AdminRewards; view: RewardView }) {
  if (view === "commitments") return <table><thead><tr><th>Founder</th><th>Order</th><th>Customer</th><th>Rate / cap</th><th>Cashback</th><th>Status</th></tr></thead><tbody>{rewards.commitments.map((item) => <tr key={item.id}><td><strong>#{String(item.founderNumber).padStart(2, "0")}</strong><small>{item.tierName}</small></td><td>{item.orderReference}<small>{money(item.purchaseAmountCents)}</small></td><td>{item.customerEmail}</td><td>{item.rateBasisPoints / 100}%<small>{money(item.capCents)} cap</small></td><td><strong>{money(item.cashbackAmountCents)}</strong></td><td><Status value={item.status} /></td></tr>)}</tbody></table>;
  return <table><thead><tr><th>Event</th><th>Order</th><th>Attempts</th><th>Error</th><th>Created</th></tr></thead><tbody>{rewards.failedEvents.map((item) => <tr key={item.id}><td><strong>{item.externalEventId}</strong></td><td>{item.orderReference}</td><td>{item.attemptCount}</td><td className="admin-error-cell">{item.lastError ?? "Unknown error"}</td><td>{dateTime(item.createdAt)}</td></tr>)}</tbody></table>;
}

function DetailSection({ title: sectionTitle, children }: { title: string; children: React.ReactNode }) { return <section className="admin-detail-section"><h3>{sectionTitle}</h3>{children}</section>; }
function Status({ value }: { value: string }) { const state = value.toLowerCase().replaceAll("_", "-").replaceAll(" ", "-"); return <span className={`admin-status ${state}`}>{value.replaceAll("_", " ")}</span>; }
function EmptyState({ label }: { label: string }) { return <div className="admin-empty">{label}</div>; }
function title(value: string) { return value === "custom-build-review" ? "Custom Build Review" : value.charAt(0).toUpperCase() + value.slice(1); }
function initials(value: string) { return value.split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join(""); }
function money(cents: number) { return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", maximumFractionDigits: 0 }).format(cents / 100); }
function moneyExact(cents: number) { return new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cents / 100); }
function date(value: string) { return new Intl.DateTimeFormat("en-AU", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value)); }
function dateTime(value: string) { return new Intl.DateTimeFormat("en-AU", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" }).format(new Date(value)); }
function formatLabel(value: string) { return value.replace(/([A-Z])/g, " $1").replace(/^./, (letter) => letter.toUpperCase()); }

export default AdminPage;
