import { myAssignments } from '../api';
import { useApi, State, Badge } from '../../admin/shared/ui';

function scopeLabel(a: { competitionId: string; categoryId?: string; roundId?: string }) {
  if (a.roundId) return 'Round-scoped';
  if (a.categoryId) return 'Category-scoped';
  return 'Competition-wide';
}

/** M12 assignments — scope is read-only; the judge can never change it. */
export default function AssignmentsPage() {
  const { data, loading, error } = useApi(() => myAssignments(), []);
  return (
    <>
      <h1 className="page-title">My Assignments</h1>
      <p className="derived">Assignment scope is set by an administrator — it grants evaluation access, not the other way around.</p>
      <div className="panel">
        <State loading={loading} error={error} empty={!data?.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr>
                <th>Competition</th><th>Scope</th><th>Category</th><th>Round</th><th>Status</th>
              </tr></thead>
              <tbody>
                {data?.map((a) => (
                  <tr key={a.id}>
                    <td>{a.competitionId.slice(0, 8)}…</td>
                    <td>{scopeLabel(a)}</td>
                    <td>{a.categoryId ? `${a.categoryId.slice(0, 8)}…` : '—'}</td>
                    <td>{a.roundId ? `${a.roundId.slice(0, 8)}…` : '—'}</td>
                    <td><Badge v={a.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </State>
      </div>
    </>
  );
}
