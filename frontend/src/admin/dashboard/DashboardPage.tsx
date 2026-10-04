import { get } from '../../api/client';
import { useApi, State } from '../shared/ui';

/** M19 dashboard — derived view, never business truth. */
export default function DashboardPage() {
  const { data, loading, error } = useApi(() =>
    get<Record<string, unknown>>('/api/v1/admin/dashboard'), []);
  return (
    <>
      <h1 className="page-title">Dashboard</h1>
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
        <p className="derived">
          Derived operational view — authoritative values live in owning modules.
        </p>
      </State>
    </>
  );
}
