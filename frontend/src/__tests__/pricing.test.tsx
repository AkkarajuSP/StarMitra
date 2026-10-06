import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PricingPage from '../pricing/PricingPage';
import type { Plan } from '../api/plans';

const PLANS: Plan[] = [
  { id: 'p1', code: 'FREE', displayName: 'Free', description: 'd',
    planType: 'SUBSCRIPTION', price: 0, currency: 'INR', billingPeriod: 'NONE',
    status: 'ACTIVE', sortOrder: 1,
    entitlements: { 'portfolio.items': '10', 'creative_rooms.active': '3',
      'analytics.tier': 'BASIC', 'discovery.visibility': 'STANDARD',
      'competition.enter': 'true' } },
  { id: 'p2', code: 'CREATOR', displayName: 'Creator', description: 'd',
    planType: 'SUBSCRIPTION', price: 99, currency: 'INR', billingPeriod: 'MONTH',
    status: 'ACTIVE', sortOrder: 2,
    entitlements: { 'portfolio.items': '50', 'creative_rooms.active': '15',
      'analytics.tier': 'ENHANCED', 'discovery.visibility': 'ENHANCED',
      'competition.enter': 'true' } },
  { id: 'p3', code: 'CREATOR_PRO', displayName: 'Creator Pro', description: 'd',
    planType: 'SUBSCRIPTION', price: 199, currency: 'INR', billingPeriod: 'MONTH',
    status: 'ACTIVE', sortOrder: 3,
    entitlements: { 'portfolio.items': '200', 'creative_rooms.active': '50',
      'analytics.tier': 'ADVANCED', 'discovery.visibility': 'ENHANCED',
      'competition.enter': 'true' } },
  { id: 'p4', code: 'COMPETITION_ORGANIZER', displayName: 'Competition Organizer',
    description: 'd', planType: 'EVENT_PACKAGE', price: 999, currency: 'INR',
    billingPeriod: 'EVENT', status: 'ACTIVE', sortOrder: 4,
    entitlements: { 'competition.create': 'true' } },
];

function stubFetch() {
  globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith('/api/v1/public/plans')) {
      return new Response(JSON.stringify(PLANS), { status: 200 });
    }
    if (url.endsWith('/api/v1/me/plan')) {
      return new Response(JSON.stringify({ planCode: 'FREE' }), { status: 200 });
    }
    return new Response('{}', { status: 404 });
  }) as unknown as typeof fetch;
}

const renderPage = (session: Parameters<typeof PricingPage>[0]['session'] = null) =>
  render(<MemoryRouter><PricingPage session={session} /></MemoryRouter>);

describe('pricing page', () => {
  beforeEach(() => { vi.restoreAllMocks(); stubFetch(); });

  it('renders the four approved plans with INR pricing', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Free' })).toBeInTheDocument());
    expect(screen.getByRole('heading', { name: 'Creator' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Creator Pro' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Competition Organizer' })).toBeInTheDocument();
    expect(screen.getByText('₹0')).toBeInTheDocument();
    expect(screen.getByText('₹99')).toBeInTheDocument();
    expect(screen.getByText('₹199')).toBeInTheDocument();
    expect(screen.getByText('₹999')).toBeInTheDocument();
    expect(screen.getAllByText('/month')).toHaveLength(2);
    expect(screen.getByText('/event')).toBeInTheDocument();
  });

  it('marks paid plans as coming soon — never as purchasable', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Creator' })).toBeInTheDocument());
    const comingSoon = screen.getAllByRole('button', { name: /coming soon/i });
    expect(comingSoon.length).toBe(3);            // CREATOR, CREATOR_PRO, ORGANIZER
    comingSoon.forEach(b => expect(b).toBeDisabled());
    expect(screen.getAllByText(/payment integration coming soon/i).length)
      .toBeGreaterThanOrEqual(3);
  });

  it('FREE links to sign-in when logged out; shows current plan when active', async () => {
    const { unmount } = renderPage(null);
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Free' })).toBeInTheDocument());
    expect(screen.getByRole('link', { name: /start free/i }))
      .toHaveAttribute('href', '/login');
    unmount();

    renderPage({ userId: 'u', email: 'a@t.dev', roles: ['USER'], accessToken: 't' });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /current plan/i })).toBeDisabled());
  });

  it('shows the feature comparison and organizer capability list', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Compare creator plans')).toBeInTheDocument());
    expect(screen.getByText('Portfolio items')).toBeInTheDocument();
    expect(screen.getByText('Creative rooms')).toBeInTheDocument();
    expect(screen.getByText('Competition creation')).toBeInTheDocument();
    expect(screen.getByText('Judges, rubrics & scoring')).toBeInTheDocument();
    expect(screen.getByText('Voting, ranking & leaderboards')).toBeInTheDocument();
  });
});
