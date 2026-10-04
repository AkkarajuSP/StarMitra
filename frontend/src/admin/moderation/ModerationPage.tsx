import { useState } from 'react';
import { get, post } from '../../api/client';
import { useApi, State, Badge, Confirm } from '../shared/ui';

interface Case { id: string; status: string; assignedModeratorId?: string }
interface CasePageData { items: Case[] }
const DECISIONS = ['NO_ACTION', 'WARNING', 'CONTENT_RESTRICTED', 'CONTENT_HIDDEN',
  'CONTENT_REMOVED', 'USER_RESTRICTED', 'USER_SUSPENDED'];

/** M18 — moderator queue. M19 orchestrates only; M18 decides. */
export default function ModerationPage() {
  const { data, loading, error, reload } = useApi(() =>
    get<CasePageData>('/api/v1/moderation/cases?status=OPEN&limit=50'), []);
  const [deciding, setDeciding] = useState<Case | null>(null);
  const [decision, setDecision] = useState(DECISIONS[0]);
  const [reason, setReason] = useState('');
  return (
    <>
      <h1 className="page-title">Moderation</h1>
      <div className="panel">
        <h2>Open cases</h2>
        <State loading={loading} error={error} empty={!data?.items.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr><th>Case</th><th>Status</th><th>Moderator</th><th /></tr></thead>
              <tbody>
                {data?.items.map((c) => (
                  <tr key={c.id}>
                    <td>{c.id.slice(0, 8)}…</td>
                    <td><Badge v={c.status} /></td>
                    <td>{c.assignedModeratorId ?? '—'}</td>
                    <td><button className="btn" onClick={() => setDeciding(c)}>Decide</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </State>
      </div>
      {deciding && (
        <Confirm
          title="Apply moderation decision"
          confirmLabel="Apply decision"
          warn="Decisions are append-only and audited. This resolves the case."
          onCancel={() => setDeciding(null)}
          onConfirm={async () => {
            await post(`/api/v1/moderation/cases/${deciding.id}/decisions`,
              { decisionType: decision, reason });
            setDeciding(null); reload();
          }}
          body={
            <>
              <div className="field">
                <label htmlFor="d-type">Decision</label>
                <select id="d-type" value={decision} onChange={(e) => setDecision(e.target.value)}>
                  {DECISIONS.map((d) => <option key={d} value={d}>{d}</option>)}
                </select>
              </div>
              <div className="field">
                <label htmlFor="d-reason">Reason (required)</label>
                <textarea id="d-reason" value={reason} required
                          onChange={(e) => setReason(e.target.value)} />
              </div>
            </>
          }
        />
      )}
    </>
  );
}
