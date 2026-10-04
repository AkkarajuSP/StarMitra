import { get } from '../../api/client';
import { useApi, State, Badge } from '../shared/ui';

interface Competition { id: string; title: string; configStatus: string;
                        participationStatus: string; roundState: string; version: number }
interface CompetitionPage { items: Competition[] }

/** M09 — competition listing with config/participation/round states.
 *  Mutations go through M09 APIs with If-Match concurrency (per backend contract). */
export default function CompetitionsPage() {
  const { data, loading, error } = useApi(() =>
    get<CompetitionPage>('/api/v1/competitions?limit=50'), []);
  return (
    <>
      <h1 className="page-title">Competitions</h1>
      <div className="panel">
        <State loading={loading} error={error} empty={!data?.items.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr><th>Title</th><th>Config</th><th>Participation</th><th>Round</th></tr></thead>
              <tbody>
                {data?.items.map((c) => (
                  <tr key={c.id}>
                    <td>{c.title}</td>
                    <td><Badge v={c.configStatus} /></td>
                    <td><Badge v={c.participationStatus} /></td>
                    <td><Badge v={c.roundState} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </State>
      </div>
      <p className="derived">State transitions (DRAFT→CONFIGURED→FROZEN, rounds) enforced by M09 — conflicts surface as 409/412.</p>
    </>
  );
}
