import { useEffect, useState } from 'react'
import { listSeries, type SeriesSummary } from '../api/catalog'
import { getProgress } from '../api/progress'
import { AppHeader } from '../components/AppHeader'
import { SeriesCard } from '../components/SeriesCard'
import { useAuth } from '../context'

export function LibraryPage() {
  const { token } = useAuth()
  const [series, setSeries] = useState<SeriesSummary[]>([])
  const [progressBySeries, setProgressBySeries] = useState<Record<string, number>>({})
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')

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

  return (
    <div className="min-h-screen bg-slate-50">
      <AppHeader />
      <main className="mx-auto max-w-5xl px-4 py-8">
        <h1 className="text-2xl font-bold">Your shows</h1>
        <p className="mt-1 text-slate-600">
          Pick a series, set how far you&apos;ve watched, and ask spoiler-safe questions.
        </p>

        {status === 'loading' && <p className="mt-6 text-slate-500">Loading…</p>}
        {status === 'error' && (
          <p className="mt-6 text-rose-600">Couldn't load the catalog. Please try again.</p>
        )}
        {status === 'ready' && series.length === 0 && (
          <p className="mt-6 text-slate-500">No series in the catalog yet.</p>
        )}
        {status === 'ready' && series.length > 0 && (
          <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {series.map((s) => (
              <SeriesCard key={s.id} series={s} progress={progressBySeries[s.id] ?? 0} />
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
