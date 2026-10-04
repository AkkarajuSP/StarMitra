import { NavLink, Outlet } from 'react-router-dom';
import type { Session } from '../../api/auth';

const NAV = [
  { to: '/judge', label: 'Dashboard', end: true },
  { to: '/judge/assignments', label: 'My Assignments' },
  { to: '/judge/submissions', label: 'My Submissions' },
  { to: '/judge/notifications', label: 'Notifications' },
];

/** M20 judge shell — focused surface; no admin navigation exposed. */
export default function JudgeShell({ session, onLogout }:
  { session: Session; onLogout: () => void }) {
  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="brand">StarMitra Judge</div>
        <nav aria-label="Judge navigation">
          {NAV.map(i => (
            <NavLink key={i.to} to={i.to} end={i.end}>{i.label}</NavLink>
          ))}
        </nav>
      </aside>
      <div className="admin-main">
        <header className="admin-top">
          <span className="crumb">Judge Portal</span>
          <div className="who">
            <span className="badge" style={{ background: 'var(--lavender)' }}>JUDGE</span>
            <span>{session.email}</span>
            <button className="btn" onClick={onLogout}>Sign out</button>
          </div>
        </header>
        <main className="admin-content"><Outlet /></main>
      </div>
    </div>
  );
}
