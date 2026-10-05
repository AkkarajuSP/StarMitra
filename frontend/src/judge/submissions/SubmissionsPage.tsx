import { Link } from 'react-router-dom';
import { mySubmissions } from '../api';
import { useApi, State, Badge } from '../../admin/shared/ui';

/** M10 FINALIZED submissions inside the judge's M12 scope — server-resolved. */
export default function SubmissionsPage() {
  const { data, loading, error } = useApi(() => mySubmissions(), []);
  return (
    <>
      <h1 className="page-title">My Submissions</h1>
      <p className="page-sub">Only submissions covered by your active assignments are shown.</p>
      <State loading={loading} error={error} empty={!data?.length}>
        <div style={{ display: 'grid', gap: 12 }}>
          {data?.map((s) => (
            <div className="sub-card" key={s.id}>
              <div className="thumb">
                <img src="/brand/StarMitra-Mark.png" alt="" aria-hidden="true" />
              </div>
              <div className="meta">
                <div className="name">Submission {s.id.slice(0, 8)}…</div>
                <div className="sub">
                  Competition {s.competitionId.slice(0, 8)}… · Round{' '}
                  {s.roundId ? `${s.roundId.slice(0, 8)}…` : '—'}
                </div>
              </div>
              <Badge v={s.state} />
              <Link to={`/judge/evaluate/${s.id}?round=${s.roundId}`}
                    className="btn primary">
                Evaluate
              </Link>
            </div>
          ))}
        </div>
      </State>
    </>
  );
}
