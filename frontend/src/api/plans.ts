import { get, post } from './client';

/** M22 pricing — backend stays authoritative; UI never invents plan rules. */

export interface Plan {
  id: string;
  code: string;
  displayName: string;
  description: string;
  planType: 'SUBSCRIPTION' | 'EVENT_PACKAGE';
  price: number;
  currency: string;
  billingPeriod: 'NONE' | 'MONTH' | 'EVENT';
  status: string;
  sortOrder: number;
  entitlements: Record<string, string>;
}

export interface EntitlementValue {
  code: string;
  kind: 'BOOLEAN' | 'INTEGER' | 'ENUM';
  value: string | null;
  limit: number | null;
  remaining: number | null;
}

export interface MyPlan {
  planCode: string;
  displayName: string;
  subscriptionStatus: string;
  source: string;
  startsAt: string;
  endsAt: string | null;
  entitlements: EntitlementValue[];
}

/** Public catalog — no auth required. */
export const listPlans = () => get<Plan[]>('/api/v1/public/plans');

/** Caller's current plan (authenticated). */
export const myPlan = () => get<MyPlan>('/api/v1/me/plan');

/**
 * Select a plan. FREE activates immediately. Priced plans fail closed —
 * the backend returns 402 PAYMENT_NOT_ENABLED (no payment rail in MVP).
 */
export const selectPlan = (planCode: string) =>
  post<MyPlan>('/api/v1/me/plan', { planCode });
