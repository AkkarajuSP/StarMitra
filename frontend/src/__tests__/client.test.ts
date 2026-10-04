import { describe, it, expect, vi, beforeEach } from 'vitest';
import { put, post } from '../api/client';

describe('api client concurrency headers', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    document.cookie = 'XSRF-TOKEN=test-xsrf';   // skip bootstrap call in assertions
    globalThis.fetch = vi.fn(async () =>
      new Response('{}', { status: 200 })) as unknown as typeof fetch;
  });

  it('sends If-Match when provided', async () => {
    await put('/api/v1/x', { a: 1 }, { ifMatch: 'W/"42"' });
    const init = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1];
    expect((init as RequestInit).headers)
      .toMatchObject({ 'If-Match': 'W/"42"' });
  });

  it('sends Idempotency-Key when provided', async () => {
    await post('/api/v1/x', {}, { idempotencyKey: 'k-1' });
    const init = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1];
    expect((init as RequestInit).headers)
      .toMatchObject({ 'Idempotency-Key': 'k-1' });
  });

  it('omits concurrency headers by default', async () => {
    await put('/api/v1/x', {});
    const init = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1];
    expect((init as RequestInit).headers)
      .not.toMatchObject({ 'If-Match': expect.anything() });
  });
});
