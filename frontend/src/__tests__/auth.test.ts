import { describe, it, expect } from 'vitest';
import { isAdmin, type Session } from '../api/auth';
import { ApiProblem } from '../api/client';

const s = (roles: string[]): Session =>
  ({ userId: 'u', email: 'a@t.dev', roles, accessToken: 't' });

describe('admin gating', () => {
  it('admin and super_admin pass', () => {
    expect(isAdmin(s(['ADMIN']))).toBe(true);
    expect(isAdmin(s(['SUPER_ADMIN']))).toBe(true);
    expect(isAdmin(s(['JUDGE', 'ADMIN']))).toBe(true);
  });
  it('non-admin roles denied — backend remains authoritative', () => {
    expect(isAdmin(s(['JUDGE']))).toBe(false);
    expect(isAdmin(s(['CREATOR', 'AUDIENCE']))).toBe(false);
    expect(isAdmin(null)).toBe(false);
  });
});

describe('api problem mapping', () => {
  it('carries status/code/detail', () => {
    const p = new ApiProblem(409, 'CONFLICT', 'stale', 'corr-1');
    expect(p.status).toBe(409);
    expect(p.code).toBe('CONFLICT');
    expect(p.message).toBe('stale');
    expect(p.correlationId).toBe('corr-1');
  });
});
