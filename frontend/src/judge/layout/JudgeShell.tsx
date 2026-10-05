import { useEffect } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import type { Session } from '../../api/auth';
import { Logo } from '../../admin/shared/ui';

const NAV = [
  { to: '/judge', label: 'Dashboard', end: true },
  { to: '/judge/assignments', label: 'My Assignments' },
  { to: '/judge/submissions', label: 'My Submissions' },
  { to: '/judge/notifications', label: 'Notifications' },
];

/** M20 judge shell — focused surface; no admin navigation exposed. */
export default function JudgeShell({ session, onLogout }:
  { session: Session; onLogout: () => void }) {
  const { pathname } = useLocation();
  const crumb = NAV.find(i => i.end ? pathname === i.to || pathname === '/judge/'
    : pathname.startsWith(i.to));
  useEffect(() => {
    document.title = `StarMitra | Judge — ${crumb?.label ?? 'Dashboard'}`;
  }, [crumb]);
  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="brand">
          <Logo variant="mark" size="small" />
          <span className="brand-text">Star<b>Mitra</b>
            <span className="portal">Judge Portal</span></span>
        </div>
        <nav aria-label="Judge navigation">
          {NAV.map(i => (
            <NavLink key={i.to} to={i.to} end={i.end}>{i.label}</NavLink>
          ))}
        </nav>
        <div className="side-foot">StarMitra · Passion to Perform</div>
      </aside>
      <div className="admin-main">
        <header className="admin-top">
          <span className="crumb">Judge / <b>{crumb?.label ?? 'Dashboard'}</b></span>
          <div className="who">
            <span className="badge judge">JUDGE</span>
            <span>{session.email}</span>
            <button className="btn" onClick={onLogout}>Sign out</button>
          </div>
        </header>
        <main className="admin-content"><Outlet /></main>
      </div>
    </div>
  );
}
