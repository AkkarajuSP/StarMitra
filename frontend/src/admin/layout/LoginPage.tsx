import { useEffect, useState } from 'react';
import { login, requestOtp } from '../../api/auth';
import type { Session } from '../../api/auth';
import { Logo, OtpInput } from '../shared/ui';

/**
 * M01 OTP sign-in — branded StarMitra auth. Two-step: request code,
 * verify. UAT: the OTP prints to the backend console (uat profile).
 */
export default function LoginPage({ onLogin }: { onLogin: (s: Session) => void }) {
  const [identifier, setIdentifier] = useState('');
  const [otp, setOtp] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { document.title = 'StarMitra — Passion to Perform'; }, []);

  const submit = async () => {
    setBusy(true); setError('');
    try {
      if (!sent) {
        await requestOtp(identifier);
        setSent(true);
      } else {
        onLogin(await login(identifier, otp));
      }
    } catch {
      setError(sent
        ? 'That code didn\'t work. Request a new one or check the digits.'
        : 'We couldn\'t send a code. Check the email and try again.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-hero">
        <img className="hero-art" src="/brand/hero.svg" alt=""
             aria-hidden="true" />
        <div><Logo surface="dark" size="large" /></div>
        <div>
          <h1>Welcome to StarMitra</h1>
          <div className="tagline">Passion to Perform</div>
          <p className="hero-note">
            Discover. Create. Perform. Connect.
          </p>
          <ul className="hero-points">
            <li>Show your talent</li>
            <li>Join competitions</li>
            <li>Collaborate and create</li>
          </ul>
        </div>
      </div>
      <div className="login-panel-wrap">
        <div className="login-panel">
          <Logo surface="light" size="normal" />
          <h2>Sign in</h2>
          <p className="sub">
            Enter your email — we'll send a one-time code.
          </p>
          {error && <div className="state error" role="alert"
                         style={{ padding: 8, textAlign: 'left' }}>{error}</div>}
          {sent && <div className="sent-ok" role="status">
            Code sent to <b>{identifier}</b></div>}
          <form onSubmit={(e) => { e.preventDefault(); submit(); }}>
            <div className="field">
              <label htmlFor="login-id">Email</label>
              <input id="login-id" type="email" value={identifier}
                     autoComplete="email" required
                     onChange={(e) => setIdentifier(e.target.value)} />
            </div>
            {sent && (
              <div className="field">
                <label id="otp-label">One-time code</label>
                <OtpInput value={otp} onChange={setOtp} autoFocus />
              </div>
            )}
            <div className="login-actions">
              <button className="btn primary" type="submit"
                      disabled={busy || !identifier || (sent && otp.length < 6)}>
                {busy ? 'Working…' : sent ? 'Verify & sign in' : 'Send code'}
              </button>
              {sent && (
                <button type="button" className="link-btn"
                        onClick={() => { setSent(false); setOtp(''); }}>
                  Use a different email
                </button>
              )}
            </div>
          </form>
          <p className="form-note">
            UAT: your code appears in the backend console — look for the
            UAT OTP banner.
          </p>
        </div>
      </div>
    </div>
  );
}
