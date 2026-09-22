import type { ApiProblem, AuthResponse } from './types'

const BASE_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

const ACCESS_KEY = 'ct.accessToken'
const REFRESH_KEY = 'ct.refreshToken'

export class ApiError extends Error {
  problem: ApiProblem

  constructor(problem: ApiProblem) {
    super(problem.code ?? problem.title ?? `HTTP ${problem.status}`)
    this.problem = problem
  }
}

export const tokenStore = {
  get access() {
    try {
      return localStorage.getItem(ACCESS_KEY)
    } catch {
      return null
    }
  },
  get refresh() {
    try {
      return localStorage.getItem(REFRESH_KEY)
    } catch {
      return null
    }
  },
  set(access: string, refresh: string) {
    try {
      localStorage.setItem(ACCESS_KEY, access)
      localStorage.setItem(REFRESH_KEY, refresh)
    } catch {
      // private mode: session won't survive a reload, which is acceptable
    }
  },
  clear() {
    try {
      localStorage.removeItem(ACCESS_KEY)
      localStorage.removeItem(REFRESH_KEY)
    } catch {
      // ignore
    }
  },
}

let onSessionExpired: (() => void) | null = null

export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler
}

// Single-flight refresh: concurrent 401s share one refresh request.
let refreshPromise: Promise<boolean> | null = null

async function tryRefresh(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = tokenStore.refresh
      if (!refreshToken) return false
      try {
        const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
        if (!res.ok) return false
        const data = (await res.json()) as AuthResponse
        tokenStore.set(data.accessToken, data.refreshToken)
        return true
      } catch {
        return false
      } finally {
        refreshPromise = null
      }
    })()
  }
  return refreshPromise
}

async function parseProblem(res: Response): Promise<ApiProblem> {
  try {
    const body = await res.json()
    return { status: res.status, title: body.title, code: body.code, fields: body.fields }
  } catch {
    return { status: res.status }
  }
}

async function authorizedFetch(
  path: string,
  method: string,
  body: unknown,
  auth: boolean,
): Promise<Response> {
  const doFetch = () => {
    const headers: Record<string, string> = {}
    // FormData sets its own multipart boundary; only JSON gets a content type.
    if (body !== undefined && !(body instanceof FormData)) {
      headers['Content-Type'] = 'application/json'
    }
    const token = tokenStore.access
    if (auth && token) headers['Authorization'] = `Bearer ${token}`
    return fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body:
        body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body),
    })
  }

  let res = await doFetch()
  if (res.status === 401 && auth) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      res = await doFetch()
    } else {
      tokenStore.clear()
      onSessionExpired?.()
      throw new ApiError({ status: 401, code: 'auth.invalidRefreshToken' })
    }
  }
  if (!res.ok) {
    throw new ApiError(await parseProblem(res))
  }
  return res
}

export async function api<T>(
  path: string,
  options: { method?: string; body?: unknown; auth?: boolean } = {},
): Promise<T> {
  const { method = 'GET', body, auth = true } = options
  const res = await authorizedFetch(path, method, body, auth)
  if (res.status === 204) {
    return undefined as T
  }
  return (await res.json()) as T
}

/** Authenticated fetch of a binary resource (e.g. the profile photo). */
export async function apiBlob(path: string): Promise<Blob> {
  const res = await authorizedFetch(path, 'GET', undefined, true)
  return res.blob()
}
