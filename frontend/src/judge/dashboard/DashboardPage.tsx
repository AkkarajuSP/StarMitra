import { Link } from 'react-router-dom';
import { myAssignments, mySubmissions } from '../api';
import { useApi, State } from '../../admin/shared/ui';

/** M20 dashboard — counts derived from authoritative M12/M10 contract data. */
export default function JudgeDashboard() {
  const a = useApi(() => myAssignments(), []);
  const s = useApi(() => mySubmissions(), []);
  const loading = a.loading || s.loading;
  const error = a.error ?? s.error;
  return (
    <>
      <h1 className="page-title">Judge Dashboard</h1>
      <p className="page-sub">Your assignments, scoped submissions, and evaluation work.</p>
      <State loading={loading} error={error}>
        <div className="cards">
          <Link to="/judge/assignments" className="stat" style={{ textDecoration: 'none' }}>
            <div className="num">{a.data?.length ?? 0}</div>
            <div className="lbl">Active assignments</div>
          </Link>
          <Link to="/judge/submissions" className="stat" style={{ textDecoration: 'none' }}>
            <div className="num">{s.data?.length ?? 0}</div>
            <div className="lbl">Submissions in my scope</div>
          </Link>
        </div>
        <p className="derived">
          Counts derive from authoritative assignment + finalized-submission contracts.
        </p>
      </State>
    </>
  );
}
