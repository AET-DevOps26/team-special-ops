import type { SeriesSummary } from '../api/catalog'
import { SeriesCard } from './SeriesCard'

/** Responsive grid of series cards with like + progress state. */
export function SeriesGrid({
  series,
  progressBySeries,
  likedIds,
  onToggleLike,
}: {
  series: SeriesSummary[]
  progressBySeries: Record<string, number>
  likedIds: Set<string>
  onToggleLike: (seriesId: string) => void
}) {
  return (
    <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
      {series.map((s) => (
        <SeriesCard
          key={s.id}
          series={s}
          progress={progressBySeries[s.id] ?? 0}
          liked={likedIds.has(s.id)}
          onToggleLike={onToggleLike}
        />
      ))}
    </div>
  )
}
