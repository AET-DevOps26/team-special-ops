import { authHeaders, fetchJson } from './client'

/** The current user's liked series ids ("My Shows"), most recently liked first. */
export function getLikes(token: string): Promise<string[]> {
  return fetchJson<string[]>('/user-progress/likes', {
    headers: authHeaders(token),
  })
}

export function likeSeries(token: string, seriesId: string): Promise<void> {
  return fetchJson<void>(`/user-progress/likes/${seriesId}`, {
    method: 'PUT',
    headers: authHeaders(token),
  })
}

export function unlikeSeries(token: string, seriesId: string): Promise<void> {
  return fetchJson<void>(`/user-progress/likes/${seriesId}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}
