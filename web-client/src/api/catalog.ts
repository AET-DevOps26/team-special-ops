import { fetchJson, getCatalogBaseUrl } from './client'
import type { components } from './types'

export type SeriesSummary = components['schemas']['SeriesSummary']
export type Episode = components['schemas']['Episode']

export function listSeries(): Promise<SeriesSummary[]> {
  return fetchJson<SeriesSummary[]>('/catalog/series', {}, getCatalogBaseUrl())
}

export function listSeriesEpisodes(id: string): Promise<Episode[]> {
  return fetchJson<Episode[]>(
    `/catalog/series/${id}/episodes`,
    {},
    getCatalogBaseUrl(),
  )
}
