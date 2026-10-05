import { useEffect, useRef, useState, type ReactNode } from 'react';
import { ApiProblem } from '../../api/client';

/** Official StarMitra brand lockup/mark — variant = the SURFACE it sits on.
 *  surface 'light' → sMLogoMainDark.png / mobileAppIconDark.png
 *  surface 'dark'  → sMLogoMainLight.png / mobileAppIconLight.png */
export function Logo({ surface = 'light', mark = false, size = 'normal' }:
  { surface?: 'light' | 'dark'; mark?: boolean;
    size?: 'small' | 'normal' | 'large' }) {
  const src = mark
    ? (surface === 'dark' ? '/brand/mobileAppIconLight.png'
                          : '/brand/mobileAppIconDark.png')
    : (surface === 'dark' ? '/brand/sMLogoMainLight.png'
                          : '/brand/sMLogoMainDark.png');
  return <span className={`logo ${size}`}>
    <img src={src} alt="StarMitra" /></span>;
}

/** Branded empty state — mark + copy; used wherever a list has no rows. */
export function EmptyState({ title = 'Nothing to show', detail }: { title?: string; detail?: string }) {
  return (
    <div className="empty-state" role="status">
      <img src="/brand/mobileAppIconDark.png" alt="" aria-hidden="true" />
      <div className="t">{title}</div>
      {detail && <div className="d">{detail}</div>}
    </div>
  );
}

/** Segmented OTP input — keyboard + paste friendly. */
export function OtpInput({ value, onChange, length = 6, autoFocus = false }:
  { value: string; onChange: (v: string) => void; length?: number; autoFocus?: boolean }) {
  const refs = useRef<(HTMLInputElement | null)[]>([]);
  const chars = value.padEnd(length).slice(0, length).split('');
  const setChar = (i: number, c: string) => {
    const next = value.padEnd(length).split('');
    next[i] = (c.replace(/\D/g, '') || ' ').slice(-1);
    onChange(next.join('').trimEnd());
    if (c && i < length - 1) refs.current[i + 1]?.focus();
  };
  return (
    <div className="otp-row" role="group" aria-label="One-time code">
      {chars.map((c, i) => (
        <input key={i} ref={(el) => { refs.current[i] = el; }}
          value={c.trim()} inputMode="numeric" maxLength={1} autoComplete="one-time-code"
          aria-label={`Digit ${i + 1}`} autoFocus={autoFocus && i === 0}
          onChange={(e) => setChar(i, e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Backspace' && !c.trim() && i > 0) refs.current[i - 1]?.focus();
          }}
          onPaste={i === 0 ? (e) => {
            e.preventDefault();
            onChange(e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length));
          } : undefined}
        />
      ))}
    </div>
  );
}

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
  if (empty) return <EmptyState />;
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
