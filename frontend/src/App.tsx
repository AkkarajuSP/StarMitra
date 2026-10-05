import { useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { setToken } from './api/client';
import { isAdmin, isJudge, type Session } from './api/auth';
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
import JudgeShell from './judge/layout/JudgeShell';
import JudgeDashboard from './judge/dashboard/DashboardPage';
import AssignmentsPage from './judge/assignments/AssignmentsPage';
import SubmissionsPage from './judge/submissions/SubmissionsPage';
import EvaluatePage from './judge/evaluation/EvaluatePage';
import JudgeNotificationsPage from './judge/notifications/NotificationsPage';

export default function App() {
  const [session, setSession] = useState<Session | null>(null);
  const doLogin = (s: Session) => { setToken(s.accessToken); setSession(s); };
  const logout = () => { setToken(null); setSession(null); };

  // Role gates are navigation UX only — backend authorization is authoritative.
  const admin = session && isAdmin(session);
  const judge = session && isJudge(session);

  return (
    <BrowserRouter>
      <Routes>
        {admin && (
          <Route path="/admin" element={<AdminShell session={session} onLogout={logout} />}>
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
        {judge && (
          <Route path="/judge" element={<JudgeShell session={session} onLogout={logout} />}>
            <Route index element={<JudgeDashboard />} />
            <Route path="assignments" element={<AssignmentsPage />} />
            <Route path="submissions" element={<SubmissionsPage />} />
            <Route path="evaluate/:submissionId" element={<EvaluatePage />} />
            <Route path="notifications" element={<JudgeNotificationsPage />} />
          </Route>
        )}
        <Route path="/login" element={<LoginPage onLogin={doLogin} />} />
        <Route path="*" element={
          !session ? <Navigate to="/login" replace />
            : admin ? <Navigate to="/admin" replace />
            : judge ? <Navigate to="/judge" replace />
            : <NoPortal />}>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

/** Authenticated account without ADMIN/JUDGE — branded dead-end, not a crash. */
function NoPortal() {
  return (
    <div className="login-page">
      <div className="login-hero" style={{ flex: 'unset', width: 320 }}>
        <img className="hero-art" src="/brand/hero.svg" alt="" aria-hidden="true" />
        <div><span className="logo small"><img src="/brand/starmitra-logo-light.svg" alt="StarMitra" /></span></div>
      </div>
      <div className="login-panel-wrap">
        <div className="login-panel">
          <div className="empty-state">
            <img src="/brand/starmitra-mark.svg" alt="" aria-hidden="true" />
            <div className="t">No portal access</div>
            <div className="d">
              This account doesn't include Admin or Judge access. The
              community experience is API-first in this release.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
