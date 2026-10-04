import { useEffect, useState, type ReactNode } from 'react';
import { ApiProblem } from '../../api/client';

export function useApi<T>(fn: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiProblem | null>(null);
  const reload = () => {
    setLoading(true);
    setError(null);
    fn().then(setData).catch((e) => setError(e instanceof ApiProblem ? e
        : new ApiProblem(0, 'NETWORK', 'Network error'))).finally(() => setLoading(false));
  };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(reload, deps);
  return { data, loading, error, reload };
}

export function State({ loading, error, empty, children }:
  { loading: boolean; error: ApiProblem | null; empty?: boolean; children: ReactNode }) {
  if (loading) return <div className="state" role="status">Loading…</div>;
  if (error) return <div className="state error" role="alert">
    {error.status === 401 || error.status === 403
      ? 'Not authorized for this operation.'
      : `Error: ${error.message}`}
  </div>;
  if (empty) return <div className="state">Nothing to show.</div>;
  return <>{children}</>;
}

export function Badge({ v }: { v: string }) {
  return <span className={`badge ${v.toLowerCase()}`}>{v}</span>;
}

export function Confirm({ title, body, warn, onConfirm, onCancel, confirmLabel = 'Confirm' }:
  { title: string; body: ReactNode; warn?: string;
    onConfirm: () => void; onCancel: () => void; confirmLabel?: string }) {
  return (
    <div className="modal-back" role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal">
        <h3>{title}</h3>
        <div>{body}</div>
        {warn && <div className="warn">{warn}</div>}
        <div className="actions">
          <button className="btn" onClick={onCancel}>Cancel</button>
          <button className="btn danger" onClick={onConfirm} autoFocus>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
