import { NavLink, Outlet, useLocation } from 'react-router-dom';
import type { Session } from '../../api/auth';

const NAV: { group: string; items: { to: string; label: string }[] }[] = [
  { group: 'Overview', items: [{ to: '/admin', label: 'Dashboard' }] },
  { group: 'Identity', items: [{ to: '/admin/users', label: 'Users & Roles' }] },
  { group: 'Talent', items: [{ to: '/admin/skills', label: 'Skill Taxonomy' }] },
  { group: 'Competitions', items: [{ to: '/admin/competitions', label: 'Competitions' }] },
  { group: 'Judging', items: [
    { to: '/admin/judges', label: 'Judges' },
    { to: '/admin/rubrics', label: 'Rubrics' },
  ]},
  { group: 'Results', items: [
    { to: '/admin/scoring', label: 'Scoring' },
    { to: '/admin/progression', label: 'Progression' },
    { to: '/admin/leaderboards', label: 'Leaderboards' },
  ]},
  { group: 'Governance', items: [
    { to: '/admin/moderation', label: 'Moderation' },
    { to: '/admin/notifications', label: 'Announcements' },
    { to: '/admin/audit', label: 'Audit' },
  ]},
];

export default function AdminShell({ session, onLogout }:
  { session: Session; onLogout: () => void }) {
  const { pathname } = useLocation();
  const crumb = NAV.flatMap(g => g.items).find(i =>
    i.to === '/admin' ? pathname === '/admin' || pathname === '/admin/'
      : pathname.startsWith(i.to));
  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="brand">StarMitra Admin</div>
        <nav aria-label="Admin navigation">
          {NAV.map(g => (
            <div key={g.group}>
              <div className="group">{g.group}</div>
              {g.items.map(i => (
                <NavLink key={i.to} to={i.to} end={i.to === '/admin'}>
                  {i.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
      </aside>
      <div className="admin-main">
        <header className="admin-top">
          <span className="crumb">Admin / {crumb?.label ?? 'Dashboard'}</span>
          <div className="who">
            <Badge v="ADMIN" />
            <span>{session.email}</span>
            <button className="btn" onClick={onLogout}>Sign out</button>
          </div>
        </header>
        <main className="admin-content"><Outlet /></main>
      </div>
    </div>
  );
}

function Badge({ v }: { v: string }) {
  return <span className="badge admin">{v}</span>;
}
