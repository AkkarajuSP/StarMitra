import { get } from '../../api/client';
import { useApi, State } from '../shared/ui';

/** M19 dashboard — derived view, never business truth. */
export default function DashboardPage() {
  const { data, loading, error } = useApi(() =>
    get<Record<string, unknown>>('/api/v1/admin/dashboard'), []);
  return (
    <>
      <h1 className="page-title">Admin Dashboard</h1>
      <p className="page-sub">StarMitra operations — moderation, judging, and platform health.</p>
      <State loading={loading} error={error}>
        <div className="cards">
          <div className="stat">
            <div className="num">{String(data?.openModerationCases ?? 0)}</div>
            <div className="lbl">Open moderation cases</div>
          </div>
          <div className="stat">
            <div className="num">{String(data?.recentAuditEntries ?? 0)}</div>
            <div className="lbl">Recent audit entries</div>
          </div>
        </div>
        <div className="panel">
          <h2>Quick actions</h2>
          <p className="derived" style={{ margin: '0 0 10px' }}>
            Derived operational view — authoritative values live in owning modules.
          </p>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <a className="btn" href="/admin/moderation">Review moderation</a>
            <a className="btn" href="/admin/competitions">Competitions</a>
            <a className="btn" href="/admin/judges">Judges</a>
            <a className="btn" href="/admin/leaderboards">Leaderboards</a>
            <a className="btn" href="/admin/audit">Audit log</a>
          </div>
        </div>
      </State>
    </>
  );
}
