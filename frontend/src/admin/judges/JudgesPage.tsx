import { get } from '../../api/client';
import { useApi, State, Badge } from '../shared/ui';

interface Judge { id: string; userId: string; status: string }
interface JudgePage { items: Judge[] }

/** M12 — judge roster. JUDGE system role ≠ judge assignment scope. */
export default function JudgesPage() {
  const { data, loading, error } = useApi(() =>
    get<JudgePage>('/api/v1/judges?limit=50'), []);
  return (
    <>
      <h1 className="page-title">Judges</h1>
      <p className="derived">Assignment scope is resolved server-side by M12 — expertise is not authorization.</p>
      <div className="panel">
        <State loading={loading} error={error} empty={!data?.items.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr><th>Judge</th><th>User</th><th>Status</th></tr></thead>
              <tbody>
                {data?.items.map((j) => (
                  <tr key={j.id}>
                    <td>{j.id.slice(0, 8)}…</td>
                    <td>{j.userId.slice(0, 8)}…</td>
                    <td><Badge v={j.status} /></td>
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
