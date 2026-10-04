import { useState } from 'react';
import { login, requestOtp } from '../../api/auth';
import type { Session } from '../../api/auth';

/** M01 OTP login — two-step; backend issues the session. */
export default function LoginPage({ onLogin }: { onLogin: (s: Session) => void }) {
  const [identifier, setIdentifier] = useState('');
  const [otp, setOtp] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  return (
    <div className="admin-shell" style={{ alignItems: 'center', justifyContent: 'center' }}>
      <div className="panel" style={{ width: 380 }}>
        <h1 className="page-title">StarMitra Admin</h1>
        {error && <div className="state error" role="alert">{error}</div>}
        <div className="field">
          <label htmlFor="login-id">Email</label>
          <input id="login-id" type="email" value={identifier}
                 onChange={(e) => setIdentifier(e.target.value)} />
        </div>
        {sent && (
          <div className="field">
            <label htmlFor="login-otp">One-time code</label>
            <input id="login-otp" value={otp} inputMode="numeric"
                   onChange={(e) => setOtp(e.target.value)} />
          </div>
        )}
        <button className="btn primary" disabled={busy || !identifier} onClick={async () => {
          setBusy(true); setError('');
          try {
            if (!sent) {
              await requestOtp(identifier);
              setSent(true);
            } else {
              onLogin(await login(identifier, otp));
            }
          } catch {
            setError('Sign-in failed. Check your credentials.');
          } finally {
            setBusy(false);
          }
        }}>{sent ? 'Verify & sign in' : 'Send code'}</button>
      </div>
    </div>
  );
}
