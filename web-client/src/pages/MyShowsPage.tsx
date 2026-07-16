import { useMemo } from 'react'
import type { SeriesSummary } from '../api/catalog'
import { AppHeader } from '../components/AppHeader'
import { SeriesGrid } from '../components/SeriesGrid'
import { useAuth } from '../context'
import { useLikes } from '../hooks/useLikes'
import { useSeriesWithProgress } from '../hooks/useSeriesWithProgress'

export function MyShowsPage() {
  const { token } = useAuth()
  const { series, progressBySeries, status: seriesStatus } = useSeriesWithProgress(token)
  const { likedIds, orderedLikedIds, status: likesStatus, toggleLike } = useLikes(token)

  // Render in like-order (newest first), hydrating each liked id from the catalog.
  const seriesById = useMemo(() => new Map(series.map((s) => [s.id, s])), [series])
  const likedSeries = orderedLikedIds
    .map((id) => seriesById.get(id))
    .filter((s): s is SeriesSummary => s !== undefined)

  // The page needs BOTH the catalog and the likes list; showing the empty state before likes
  // have loaded would falsely tell a user with likes that they have none.
  const status =
    seriesStatus === 'error' || likesStatus === 'error'
      ? 'error'
      : seriesStatus === 'ready' && likesStatus === 'ready'
        ? 'ready'
        : 'loading'

  return (
    <div className="min-h-screen bg-slate-50">
      <AppHeader />
      <main className="mx-auto max-w-5xl px-4 py-8">
        <h1 className="text-2xl font-bold">My Shows</h1>
        <p className="mt-1 text-slate-600">Series you&apos;ve liked.</p>

        {status === 'loading' && <p className="mt-6 text-slate-500">Loading…</p>}
        {status === 'error' && (
          <p className="mt-6 text-rose-600">Couldn't load your shows. Please try again.</p>
        )}
        {status === 'ready' && likedSeries.length === 0 && (
          <p className="mt-6 text-slate-500">
            You haven&apos;t liked any shows yet. Tap the ♡ on a series to add it here.
          </p>
        )}
        {status === 'ready' && likedSeries.length > 0 && (
          <SeriesGrid
            series={likedSeries}
            progressBySeries={progressBySeries}
            likedIds={likedIds}
            onToggleLike={toggleLike}
          />
        )}
      </main>
    </div>
  )
}
