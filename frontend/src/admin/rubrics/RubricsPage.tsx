import { get } from '../../api/client';
import { useApi, State, Badge } from '../shared/ui';

interface Template { id: string; name: string; status: string }
interface TemplatePage { items: Template[] }

/** M13 — rubric templates. Published versions are IMMUTABLE — editing a
 *  published version is impossible by design (new draft version instead). */
export default function RubricsPage() {
  const { data, loading, error } = useApi(() =>
    get<TemplatePage>('/api/v1/evaluations/templates?limit=50'), []);
  return (
    <>
      <h1 className="page-title">Rubrics</h1>
      <p className="derived">
        Published versions are immutable — publish requires weight total = 100% and
        valid criteria (M13-enforced).
      </p>
      <div className="panel">
        <State loading={loading} error={error} empty={!data?.items.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr><th>Template</th><th>Status</th></tr></thead>
              <tbody>
                {data?.items.map((t) => (
                  <tr key={t.id}><td>{t.name}</td><td><Badge v={t.status} /></td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </State>
      </div>
    </>
  );
}
