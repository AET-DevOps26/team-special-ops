import { AppHeader } from '../components/AppHeader'
import { SeriesGrid } from '../components/SeriesGrid'
import { useAuth } from '../context'
import { useLikes } from '../hooks/useLikes'
import { useSeriesWithProgress } from '../hooks/useSeriesWithProgress'

export function LibraryPage() {
  const { token } = useAuth()
  const { series, progressBySeries, status } = useSeriesWithProgress(token)
  const { likedIds, toggleLike } = useLikes(token)

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
          <SeriesGrid
            series={series}
            progressBySeries={progressBySeries}
            likedIds={likedIds}
            onToggleLike={toggleLike}
          />
        )}
      </main>
    </div>
  )
}
