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

export async function api<T = unknown>(
  path: string,
  opts: { method?: string; body?: unknown; ifMatch?: string; idempotencyKey?: string } = {},
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  if (opts.ifMatch) headers['If-Match'] = opts.ifMatch;
  if (opts.idempotencyKey) headers['Idempotency-Key'] = opts.idempotencyKey;
  const res = await fetch(path, {
    method: opts.method ?? 'GET',
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
