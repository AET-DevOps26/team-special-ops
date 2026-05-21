import { authHeaders, fetchJson } from './client'
import type { components } from './types'

export type SignupRequest = components['schemas']['SignupRequest']
export type LoginRequest = components['schemas']['LoginRequest']
export type AuthResponse = components['schemas']['AuthResponse']
export type UserSummary = components['schemas']['UserSummary']

export function signup(body: SignupRequest): Promise<AuthResponse> {
  return fetchJson<AuthResponse>('/user-progress/auth/signup', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function login(body: LoginRequest): Promise<AuthResponse> {
  return fetchJson<AuthResponse>('/user-progress/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function getMe(token: string): Promise<UserSummary> {
  return fetchJson<UserSummary>('/user-progress/auth/me', {
    headers: authHeaders(token),
  })
}
