import type { components } from './types'

type ApiError = components['schemas']['Error']

export function getBaseUrl(): string {
  return import.meta.env.VITE_USER_PROGRESS_URL ?? 'http://localhost:8081'
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
): Promise<T> {
  const url = `${getBaseUrl()}${path}`
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
