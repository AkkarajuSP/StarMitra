import { get, post, put, del, type Paged } from './api';

/** M01 — OTP auth */
export interface AuthSession {
  accessToken: string; expiresIn: number;
  user: { id: string; email: string; status: string; systemRoles: string[] };
}
export const requestOtp = (identifier: string) =>
  post<void>('/api/v1/auth/otp/request', { identifier, channel: 'EMAIL' });
export const verifyOtp = (identifier: string, otp: string) =>
  post<AuthSession>('/api/v1/auth/otp/verify', { identifier, otp });

/** M02/M03 — profile + skills */
export interface SkillView { skillId: string; name: string; proficiency: string }
export interface Profile {
  userId: string; displayName: string; bio?: string; location?: string;
  avatarMediaId?: string; visibilityState: string; skills: SkillView[];
}
export const myProfile = () => get<Profile>('/api/v1/profiles/me');
export const updateProfile = (b: Partial<Profile>) => put<Profile>('/api/v1/profiles/me', b);
export const publicProfile = (userId: string) =>
  get<Partial<Profile>>(`/api/v1/profiles/${userId}`);

export interface Skill { id: string; name: string; status: string }
export const skills = () => get<Paged<Skill>>('/api/v1/skills?limit=100');
export const addMySkill = (skillId: string, proficiency: string) =>
  put<void>(`/api/v1/users/me/skills/${skillId}`, { proficiency });
export const removeMySkill = (skillId: string) =>
  del<void>(`/api/v1/users/me/skills/${skillId}`);

/** M05 — discovery / feed / search */
export interface SearchItem extends Record<string, unknown> {}
export const feed = () => get<Paged<SearchItem>>('/api/v1/feed?limit=30');
export const discover = () => get<Paged<SearchItem>>('/api/v1/discovery?limit=30');
export const search = (q: string) =>
  get<Paged<SearchItem>>(`/api/v1/search?q=${encodeURIComponent(q)}&limit=30`);

/** M09/M10 — competitions + submissions */
export interface Competition {
  id: string; title: string; configStatus: string;
  participationStatus: string; roundState: string; version: number;
}
export interface Category { id: string; name: string }
export interface Round { id: string; sequence: number; name?: string; status?: string;
  startAt?: string; endAt?: string }
export const competitions = () => get<Paged<Competition>>('/api/v1/competitions?limit=30');
export const competition = (id: string) => get<Competition>(`/api/v1/competitions/${id}`);
export const participants = (id: string) =>
  get<Paged<Participant>>(`/api/v1/competitions/${id}/participants?limit=50`);
export const registerParticipant = (id: string, categoryId?: string) =>
  post<Participant>(`/api/v1/competitions/${id}/participants/register`,
    { participantType: 'USER', categoryId });
export interface Participant { id: string; participantType: string; userId?: string;
  status?: string; categoryId?: string }
export interface Submission { id: string; participantId: string; competitionId: string;
  roundId?: string; state: string }
export const competitionSubmissions = (id: string) =>
  get<Paged<Submission>>(`/api/v1/competitions/${id}/submissions/list?limit=30`);
export const createSubmission = (competitionId: string, participantId: string, roundId: string) =>
  post<Submission>(`/api/v1/competitions/${competitionId}/submissions`,
    { participantId, roundId });
export const submitSubmission = (id: string) =>
  post<Submission>(`/api/v1/submissions/${id}/submit`);
export const finalizeSubmission = (id: string) =>
  post<Submission>(`/api/v1/submissions/${id}/finalize`);

/** M11 — voting */
export const castVote = (submissionId: string, idempotencyKey: string) =>
  post('/api/v1/votes', { submissionId, idempotencyKey });
export const voteCount = (submissionId: string) =>
  get<{ submissionId: string; count: number }>(
    `/api/v1/votes/counts?submissionId=${submissionId}`);

/** M16 — leaderboards */
export interface LeaderboardEntry { rank: number; entryRef: string;
  display?: Record<string, unknown> }
export const leaderboard = (competitionId: string) =>
  get<Paged<LeaderboardEntry>>(
    `/api/v1/leaderboards?competitionId=${competitionId}&limit=50`);

/** M06 — connect */
export interface Conversation { id: string; type: string; status: string;
  projectId?: string }
export interface Message { id: string; conversationId: string; senderId: string;
  sequence: number; body?: string; sentAt?: string }
export const conversations = () => get<Paged<Conversation>>('/api/v1/conversations?limit=30');
export const messages = (id: string) =>
  get<Paged<Message>>(`/api/v1/conversations/${id}/messages?limit=50`);
export const sendMessage = (id: string, body: string) =>
  post<Message>(`/api/v1/conversations/${id}/messages`, { body });
export const markRead = (id: string, upToSequence: number) =>
  post<void>(`/api/v1/conversations/${id}/read`, { upToSequence });

/** M07 — rooms */
export interface Room { id: string; name: string; status: string;
  visibility?: string; description?: string }
export const rooms = () => get<Paged<Room>>('/api/v1/rooms?limit=30');
export const room = (id: string) => get<Room>(`/api/v1/rooms/${id}`);
export const roomMembers = (id: string) =>
  get<{ userId: string; status: string }[]>(`/api/v1/rooms/${id}/members`);

/** M08 — portfolio */
export interface Portfolio { id: string; userId: string; title?: string; status: string }
export interface PortfolioItem { id: string; title: string; description?: string;
  skillId?: string; status?: string }
export const myPortfolio = () => get<Portfolio>('/api/v1/portfolios/me');
export const myPortfolioItems = () => get<PortfolioItem[]>('/api/v1/portfolios/me/items');
export const publicPortfolio = (userId: string) =>
  get<Portfolio>(`/api/v1/portfolios/${userId}`);

/** M17 — notifications */
export interface Notification { id: string; type: string; body: string;
  deepLink?: string; state: string; createdAt: string }
export const notifications = () =>
  get<Paged<Notification>>('/api/v1/notifications?limit=50');
export const markNotificationRead = (id: string) =>
  post<void>(`/api/v1/notifications/${id}/read`);
export const markAllRead = () => post<void>('/api/v1/notifications/read-all');

/** M21 — social */
export const follow = (userId: string) => post<void>(`/api/v1/social/follows/${userId}`);
export const unfollow = (userId: string) => del<void>(`/api/v1/social/follows/${userId}`);
export const following = () =>
  get<Paged<{ userId: string; followedAt: string }>>('/api/v1/social/follows/me?limit=50');
export const like = (targetType: string, targetId: string, idempotencyKey: string) =>
  post('/api/v1/social/likes', { targetType, targetId, idempotencyKey });
export const comment = (targetType: string, targetId: string, body: string) =>
  post('/api/v1/social/comments', { targetType, targetId, body });
export const comments = (targetType: string, targetId: string) =>
  get<Paged<{ id: string; authorId: string; body: string; createdAt: string }>>(
    `/api/v1/social/comments?targetType=${targetType}&targetId=${targetId}&limit=30`);
