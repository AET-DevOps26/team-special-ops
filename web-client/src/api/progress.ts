import { authHeaders, fetchJson } from './client'
import type { components } from './types'

export type ProgressEntry = components['schemas']['ProgressEntry']
export type UpdateProgressRequest = components['schemas']['UpdateProgressRequest']

export function getProgress(token: string): Promise<ProgressEntry[]> {
  return fetchJson<ProgressEntry[]>('/user-progress/progress', {
    headers: authHeaders(token),
  })
}

export function updateProgress(
  token: string,
  body: UpdateProgressRequest,
): Promise<ProgressEntry> {
  return fetchJson<ProgressEntry>('/user-progress/progress', {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(body),
  })
}
