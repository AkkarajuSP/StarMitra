/** Typed API client — calls owning-module endpoints only; no business rules. */

export class ApiProblem extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public correlationId?: string,
  ) {
    super(message);
  }
}

let accessToken: string | null = null;
export const setToken = (t: string | null) => (accessToken = t);
export const getToken = () => accessToken;

/** Cookie-carried mutations need X-XSRF-TOKEN; bootstrap it once. Bearer requests are CSRF-exempt. */
let csrfReady: Promise<void> | null = null;
function readCookie(name: string): string | null {
  const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
  return m ? decodeURIComponent(m[1]) : null;
}
async function ensureCsrf(): Promise<void> {
  if (readCookie('XSRF-TOKEN')) return;
  if (!csrfReady) {
    csrfReady = fetch('/api/v1/public/csrf', { credentials: 'include' })
      .then(() => undefined)
      .catch(() => { csrfReady = null; });   // retry on next mutation
  }
  await csrfReady;
}

export async function api<T = unknown>(
  path: string,
  opts: { method?: string; body?: unknown; ifMatch?: string; idempotencyKey?: string } = {},
): Promise<T> {
  const method = opts.method ?? 'GET';
  if (method !== 'GET' && !accessToken) await ensureCsrf();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const xsrf = readCookie('XSRF-TOKEN');
  if (method !== 'GET' && !accessToken && xsrf) headers['X-XSRF-TOKEN'] = xsrf;
  if (opts.ifMatch) headers['If-Match'] = opts.ifMatch;
  if (opts.idempotencyKey) headers['Idempotency-Key'] = opts.idempotencyKey;
  const res = await fetch(path, {
    method,
    headers,
    body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
    credentials: 'include',
  });
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    const p = data ?? {};
    throw new ApiProblem(res.status, p.code ?? `HTTP_${res.status}`,
      p.detail ?? p.title ?? `Request failed (${res.status})`, p.correlationId);
  }
  return data as T;
}

export const get = <T>(path: string) => api<T>(path);
export const post = <T>(path: string, body?: unknown, extra?: { idempotencyKey?: string; ifMatch?: string }) =>
  api<T>(path, { method: 'POST', body, ...extra });
export const put = <T>(path: string, body?: unknown, extra?: { ifMatch?: string }) =>
  api<T>(path, { method: 'PUT', body, ...extra });
export const del = <T>(path: string) => api<T>(path, { method: 'DELETE' });
