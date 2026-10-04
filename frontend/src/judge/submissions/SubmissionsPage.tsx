import { Link } from 'react-router-dom';
import { mySubmissions } from '../api';
import { useApi, State, Badge } from '../../admin/shared/ui';

/** M10 FINALIZED submissions inside the judge's M12 scope — server-resolved. */
export default function SubmissionsPage() {
  const { data, loading, error } = useApi(() => mySubmissions(), []);
  return (
    <>
      <h1 className="page-title">My Submissions</h1>
      <p className="derived">Only submissions covered by your active assignments are shown.</p>
      <div className="panel">
        <State loading={loading} error={error} empty={!data?.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr>
                <th>Submission</th><th>Competition</th><th>Round</th><th>State</th><th />
              </tr></thead>
              <tbody>
                {data?.map((s) => (
                  <tr key={s.id}>
                    <td>{s.id.slice(0, 8)}…</td>
                    <td>{s.competitionId.slice(0, 8)}…</td>
                    <td>{s.roundId ? `${s.roundId.slice(0, 8)}…` : '—'}</td>
                    <td><Badge v={s.state} /></td>
                    <td>
                      <Link to={`/judge/evaluate/${s.id}?round=${s.roundId}`} className="btn">
                        Evaluate
                      </Link>
                    </td>
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
