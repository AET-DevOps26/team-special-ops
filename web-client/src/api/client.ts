import type { components } from './types'

type ApiError = components['schemas']['Error']

// Default to same-origin relative paths (''): the SPA is served behind a
// reverse proxy (nginx in the image, Vite's dev proxy in `pnpm dev`) that
// routes /user-progress and /catalog to the backend services, so no CORS is
// involved. Set VITE_*_URL to point at absolute backend URLs instead.
export function getBaseUrl(): string {
  return import.meta.env.VITE_USER_PROGRESS_URL ?? ''
}

export function getCatalogBaseUrl(): string {
  return import.meta.env.VITE_CATALOG_URL ?? ''
}

export class ApiRequestError extends Error {
  readonly status: number
  readonly code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
  }
}

export async function fetchJson<T>(
  path: string,
  options: RequestInit = {},
  baseUrl: string = getBaseUrl(),
): Promise<T> {
  const url = `${baseUrl}${path}`
  const headers = new Headers(options.headers)

  if (options.body != null && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(url, { ...options, headers })

  if (!response.ok) {
    let message =
      response.statusText || `Request failed (${response.status})`
    let code: string | undefined
    try {
      const body = (await response.json()) as ApiError
      if (body.message) message = body.message
      code = body.code
    } catch {
      // ignore non-JSON bodies
    }
    throw new ApiRequestError(response.status, message, code)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export function authHeaders(token: string): HeadersInit {
  return { Authorization: `Bearer ${token}` }
}
