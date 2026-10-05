import Constants from 'expo-constants';

export const API_BASE: string =
  (Constants.expoConfig?.extra as { apiBase?: string } | undefined)?.apiBase
  ?? 'http://10.0.2.2:9090'; // Android emulator fallback

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) {
    super(message);
  }
}

let token: string | null = null;
export const setToken = (t: string | null) => { token = t; };

/** Cookie-carried CSRF bootstrap — needed only for unauthenticated mutations
 *  (OTP request/verify); Bearer requests are CSRF-exempt per SecurityConfig. */
let csrfToken: string | null = null;
async function ensureCsrf(): Promise<void> {
  if (csrfToken || token) return;
  try {
    const res = await fetch(`${API_BASE}/api/v1/public/csrf`);
    const body = (await res.json()) as { token?: string };
    csrfToken = body.token ?? null;
  } catch {
    csrfToken = null;
  }
}

async function req<T>(method: string, path: string, body?: unknown): Promise<T> {
  if (method !== 'GET' && !token) await ensureCsrf();
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(csrfToken && !token
          ? { 'X-XSRF-TOKEN': csrfToken, Cookie: `XSRF-TOKEN=${csrfToken}` }
          : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Network unavailable — check your connection.');
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  let json: unknown = null;
  try { json = text ? JSON.parse(text) : null; } catch { json = null; }
  if (!res.ok) {
    const p = json as { status?: number; code?: string; message?: string; title?: string } | null;
    throw new ApiError(
      res.status,
      p?.code ?? `HTTP_${res.status}`,
      p?.message ?? p?.title ?? `Request failed (${res.status})`,
    );
  }
  return json as T;
}

export const get = <T>(p: string) => req<T>('GET', p);
export const post = <T>(p: string, b?: unknown) => req<T>('POST', p, b);
export const put = <T>(p: string, b?: unknown) => req<T>('PUT', p, b);
export const del = <T>(p: string) => req<T>('DELETE', p);

export interface PageMeta { nextCursor: string | null; hasMore: boolean; total?: number }
export interface Paged<T> { items: T[]; page?: PageMeta }
