import { useEffect, useState } from 'react'
import { listSeries, type SeriesSummary } from '../api/catalog'
import { getProgress } from '../api/progress'

type Status = 'loading' | 'ready' | 'error'

/** Loads the catalog plus the current user's watch progress, keyed by series id. */
export function useSeriesWithProgress(token: string | null) {
  const [series, setSeries] = useState<SeriesSummary[]>([])
  const [progressBySeries, setProgressBySeries] = useState<Record<string, number>>({})
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    if (!token) return
    let cancelled = false
    setStatus('loading')

    Promise.all([listSeries(), getProgress(token)])
      .then(([seriesList, progress]) => {
        if (cancelled) return
        const byId: Record<string, number> = {}
        for (const entry of progress) byId[entry.seriesId] = entry.episodeIndex
        setSeries(seriesList)
        setProgressBySeries(byId)
        setStatus('ready')
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [token])

  return { series, progressBySeries, status }
}
