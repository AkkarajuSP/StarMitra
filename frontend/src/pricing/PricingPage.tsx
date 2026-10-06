import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiProblem } from '../api/client';
import { listPlans, myPlan, selectPlan, type Plan } from '../api/plans';
import type { Session } from '../api/auth';
import { Logo, useApi, State } from '../admin/shared/ui';

/**
 * M22 pricing — public catalog + plan selection. Backend stays
 * authoritative: FREE activates via POST /me/plan; priced plans are
 * shown "Coming soon" and never pretend a purchase occurred.
 */
export default function PricingPage({ session }: { session: Session | null }) {
  const { data: plans, loading, error } = useApi(listPlans);
  const [current, setCurrent] = useState<string | null>(null);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    document.title = 'StarMitra | Pricing — Choose Your Journey';
    if (session) myPlan().then(p => setCurrent(p.planCode)).catch(() => {});
  }, [session]);

  const choose = async (p: Plan) => {
    setNotice('');
    if (!session) return;                                  // CTA links to /login instead
    setBusy(true);
    try {
      const mine = await selectPlan(p.code);
      setCurrent(mine.planCode);
      setNotice(`${mine.displayName} is now active on your account.`);
    } catch (e) {
      setNotice(e instanceof ApiProblem && e.code === 'PAYMENT_NOT_ENABLED'
        ? 'Payment integration coming soon — paid plans are not purchasable yet.'
        : 'We could not update your plan. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  const creators = (plans ?? []).filter(p => p.planType === 'SUBSCRIPTION');
  const organizers = (plans ?? []).filter(p => p.planType === 'EVENT_PACKAGE');
  const price = (p: Plan) =>
    `₹${Number(p.price).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
  const cadence = (p: Plan) =>
    p.billingPeriod === 'MONTH' ? '/month' : p.billingPeriod === 'EVENT' ? '/event' : '';

  return (
    <div className="pricing-page">
      <header className="pricing-top">
        <Link to="/" aria-label="StarMitra home">
          <Logo surface="dark" size="small" />
        </Link>
        <nav className="pricing-nav">
          {session
            ? <span className="pricing-who">{session.email}</span>
            : <Link className="btn primary" to="/login">Sign in</Link>}
        </nav>
      </header>

      <section className="pricing-hero">
        <h1>Choose Your StarMitra Journey</h1>
        <p>Start free. Showcase your talent. Grow your creative presence.</p>
      </section>

      <State loading={loading} error={error}>
        {notice && <div className="pricing-notice" role="status">{notice}</div>}

        <div className="plan-grid" role="list">
          {creators.map(p => {
            const paid = p.price > 0;
            const isCurrent = current === p.code;
            const featured = p.code === 'CREATOR_PRO';
            return (
              <article key={p.code} role="listitem"
                       className={`plan-card${featured ? ' featured' : ''}`}>
                {featured && <div className="plan-flag">Most popular</div>}
                <h2>{p.displayName}</h2>
                <div className="plan-price">
                  <span className="amt">{price(p)}</span>
                  <span className="per">{cadence(p) || '/forever'}</span>
                </div>
                <p className="plan-desc">{p.description}</p>
                <PlanPoints plan={p} />
                {isCurrent ? (
                  <button className="btn" disabled>Current plan</button>
                ) : paid ? (
                  <button className="btn" disabled title="Payment integration coming soon">
                    Coming soon
                  </button>
                ) : session ? (
                  <button className="btn primary" disabled={busy}
                          onClick={() => choose(p)}>Start free</button>
                ) : (
                  <Link className="btn primary" to="/login">Start free</Link>
                )}
                {paid && <div className="plan-note">Payment integration coming soon</div>}
              </article>
            );
          })}
          {organizers.map(p => (
            <article key={p.code} role="listitem" className="plan-card organizer">
              <div className="plan-flag alt">For organizers</div>
              <h2>{p.displayName}</h2>
              <div className="plan-price">
                <span className="per">From</span>
                <span className="amt">{price(p)}</span>
                <span className="per">{cadence(p)}</span>
              </div>
              <p className="plan-desc">{p.description}</p>
              <ul className="plan-points">
                <li>Competition creation</li>
                <li>Multiple categories &amp; rounds</li>
                <li>Judges, rubrics &amp; scoring</li>
                <li>Voting, ranking &amp; leaderboards</li>
              </ul>
              <button className="btn" disabled title="Payment integration coming soon">
                Coming soon
              </button>
              <div className="plan-note">Payment integration coming soon</div>
            </article>
          ))}
        </div>

        <section className="compare">
          <h2>Compare creator plans</h2>
          <div className="table-wrap">
            <table className="list compare-table">
              <thead>
                <tr>
                  <th>Capability</th>
                  {creators.map(p => <th key={p.code}>{p.displayName}</th>)}
                </tr>
              </thead>
              <tbody>
                <CompareRow label="Profile" plans={creators} render={() => '✓'} />
                <CompareRow label="Multiple skills" plans={creators} render={() => '✓'} />
                <CompareRow label="Portfolio items" plans={creators}
                            render={p => p.entitlements['portfolio.items'] ?? '✓'} />
                <CompareRow label="Media" plans={creators} render={() => '✓'} />
                <CompareRow label="Discovery" plans={creators}
                            render={p => label(p.entitlements['discovery.visibility'], 'STANDARD')} />
                <CompareRow label="Creative rooms" plans={creators}
                            render={p => p.entitlements['creative_rooms.active'] ?? '✓'} />
                <CompareRow label="Analytics" plans={creators}
                            render={p => label(p.entitlements['analytics.tier'], 'BASIC')} />
                <CompareRow label="Competitions" plans={creators} render={() => '✓'} />
              </tbody>
            </table>
          </div>
          <p className="derived">
            Audience access and audience voting are free. Paid-plan checkout is not
            enabled yet — selecting a paid plan never charges you.
          </p>
        </section>
      </State>
    </div>
  );
}

function label(v: string | undefined, fallback: string) {
  if (!v) return fallback;
  return v.charAt(0) + v.slice(1).toLowerCase();
}

/** Entitlement-driven highlights — values come from the backend, not copy. */
function PlanPoints({ plan }: { plan: Plan }) {
  const e = plan.entitlements;
  const points = [
    'Public creator profile',
    e['portfolio.items'] && `${e['portfolio.items']} portfolio items`,
    e['creative_rooms.active'] && `${e['creative_rooms.active']} active creative rooms`,
    e['analytics.tier'] && `${label(e['analytics.tier'], '')} analytics`,
    e['discovery.visibility'] && `${label(e['discovery.visibility'], '')} discovery visibility`,
    e['competition.enter'] === 'true' && 'Enter competitions',
  ].filter(Boolean) as string[];
  return (
    <ul className="plan-points">
      {points.map(pt => <li key={pt}>{pt}</li>)}
    </ul>
  );
}

function CompareRow({ label: lbl, plans, render }:
  { label: string; plans: Plan[]; render: (p: Plan) => string }) {
  return (
    <tr>
      <td>{lbl}</td>
      {plans.map(p => <td key={p.code}>{render(p)}</td>)}
    </tr>
  );
}
