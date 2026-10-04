import { useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { setToken } from './api/client';
import { isAdmin, type Session } from './api/auth';
import AdminShell from './admin/layout/AdminShell';
import LoginPage from './admin/layout/LoginPage';
import DashboardPage from './admin/dashboard/DashboardPage';
import AuditPage from './admin/audit/AuditPage';
import ModerationPage from './admin/moderation/ModerationPage';
import LeaderboardsPage from './admin/leaderboards/LeaderboardsPage';
import SkillsPage from './admin/skills/SkillsPage';
import UsersPage from './admin/users/UsersPage';
import CompetitionsPage from './admin/competitions/CompetitionsPage';
import JudgesPage from './admin/judges/JudgesPage';
import RubricsPage from './admin/rubrics/RubricsPage';
import ScoringPage from './admin/scoring/ScoringPage';
import ProgressionPage from './admin/progression/ProgressionPage';
import NotificationsPage from './admin/notifications/NotificationsPage';

export default function App() {
  const [session, setSession] = useState<Session | null>(null);
  const authed = session && isAdmin(session);           // backend still authoritative

  const doLogin = (s: Session) => { setToken(s.accessToken); setSession(s); };
  const logout = () => { setToken(null); setSession(null); };

  return (
    <BrowserRouter>
      <Routes>
        {!authed && <Route path="*" element={
          session && !isAdmin(session)
            ? <div className="state error" role="alert">
                This account does not have admin access.
              </div>
            : <LoginPage onLogin={doLogin} />
        } />}
        {authed && (
          <Route path="/admin" element={
            <AdminShell session={session} onLogout={logout} />}>
            <Route index element={<DashboardPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="skills" element={<SkillsPage />} />
            <Route path="competitions" element={<CompetitionsPage />} />
            <Route path="judges" element={<JudgesPage />} />
            <Route path="rubrics" element={<RubricsPage />} />
            <Route path="scoring" element={<ScoringPage />} />
            <Route path="progression" element={<ProgressionPage />} />
            <Route path="leaderboards" element={<LeaderboardsPage />} />
            <Route path="moderation" element={<ModerationPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="audit" element={<AuditPage />} />
          </Route>
        )}
        <Route path="*" element={<Navigate to="/admin" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
