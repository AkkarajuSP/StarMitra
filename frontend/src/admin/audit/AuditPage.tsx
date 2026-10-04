import { useState } from 'react';
import { get } from '../../api/client';
import { useApi, State } from '../shared/ui';

interface AuditItem {
  module: string; action: string; actorId: string;
  targetType: string; targetId: string; createdAt: string;
}
interface AuditPageData { items: AuditItem[] }

/** M19 audit search — read-only over kernel audit_log; never editable. */
export default function AuditPage() {
  const [module, setModule] = useState('');
  const [actorId, setActorId] = useState('');
  const { data, loading, error } = useApi(() => {
    const q = new URLSearchParams();
    if (module) q.set('module', module);
    if (actorId) q.set('actorId', actorId);
    q.set('limit', '50');
    return get<AuditPageData>(`/api/v1/admin/audit?${q}`);
  }, [module, actorId]);
  return (
    <>
      <h1 className="page-title">Audit</h1>
      <div className="panel">
        <div className="filters">
          <div className="field">
            <label htmlFor="f-module">Module</label>
            <input id="f-module" value={module} placeholder="e.g. M18"
                   onChange={(e) => setModule(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="f-actor">Actor ID</label>
            <input id="f-actor" value={actorId} placeholder="UUID"
                   onChange={(e) => setActorId(e.target.value)} />
          </div>
        </div>
        <State loading={loading} error={error} empty={!data?.items.length}>
          <div className="table-wrap">
            <table className="list" aria-label="Audit entries">
              <thead><tr>
                <th>Time</th><th>Module</th><th>Action</th><th>Actor</th><th>Target</th>
              </tr></thead>
              <tbody>
                {data?.items.map((a, i) => (
                  <tr key={i}>
                    <td>{new Date(a.createdAt).toLocaleString()}</td>
                    <td>{a.module}</td>
                    <td>{a.action}</td>
                    <td>{a.actorId || '—'}</td>
                    <td>{a.targetType ? `${a.targetType}/${a.targetId}` : '—'}</td>
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
