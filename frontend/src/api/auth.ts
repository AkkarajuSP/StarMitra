import { post } from './client';

export interface CurrentUser {
  id: string;
  email: string;
  status: string;
  systemRoles: string[];
}
export interface AuthSession {
  accessToken: string;
  expiresIn: number;
  user: CurrentUser;
}
export interface Session {
  userId: string;
  email: string;
  roles: string[];
  accessToken: string;
}

/** M01 OTP login — backend stays authoritative; UI never trusts client state. */
export async function requestOtp(identifier: string): Promise<void> {
  await post('/api/v1/auth/otp/request', { identifier, channel: 'EMAIL' });
}

export async function login(identifier: string, otp: string): Promise<Session> {
  const s = await post<AuthSession>('/api/v1/auth/otp/verify', { identifier, otp });
  return {
    userId: s.user.id,
    email: s.user.email,
    roles: s.user.systemRoles,
    accessToken: s.accessToken,
  };
}

export function isAdmin(s: Session | null): boolean {
  return !!s && (s.roles.includes('ADMIN') || s.roles.includes('SUPER_ADMIN'));
}

export function isJudge(s: Session | null): boolean {
  return !!s && s.roles.includes('JUDGE');
}
