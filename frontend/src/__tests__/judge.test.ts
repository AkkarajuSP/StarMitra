import { describe, it, expect } from 'vitest';
import { isJudge, isAdmin, type Session } from '../api/auth';

const s = (roles: string[]): Session =>
  ({ userId: 'u', email: 'j@t.dev', roles, accessToken: 't' });

describe('judge gating', () => {
  it('JUDGE role reaches the portal', () => {
    expect(isJudge(s(['JUDGE']))).toBe(true);
  });
  it('non-judge roles denied — scope check is server-side', () => {
    expect(isJudge(s(['CREATOR']))).toBe(false);
    expect(isJudge(s(['ADMIN']))).toBe(false);            // admin ≠ judge unless assigned
    expect(isJudge(null)).toBe(false);
  });
  it('admin and judge gates are independent', () => {
    expect(isAdmin(s(['JUDGE']))).toBe(false);            // judge ≠ admin
    expect(isJudge(s(['ADMIN', 'JUDGE']))).toBe(true);
    expect(isAdmin(s(['ADMIN', 'JUDGE']))).toBe(true);
  });
});

describe('score validation', () => {
  const inRange = (v: number, max?: number) =>
    Number.isFinite(v) && v >= 0 && (max === undefined || v <= max);
  it('accepts within-range scores', () => {
    expect(inRange(8, 10)).toBe(true);
    expect(inRange(0, 10)).toBe(true);
  });
  it('rejects out-of-range / non-numeric scores', () => {
    expect(inRange(11, 10)).toBe(false);
    expect(inRange(-1, 10)).toBe(false);
    expect(inRange(NaN, 10)).toBe(false);
  });
});
