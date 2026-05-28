import { Link } from 'react-router-dom'
import type { SeriesSummary } from '../api/catalog'

export function SeriesCard({
  series,
  progress,
}: {
  series: SeriesSummary
  progress: number
}) {
  const pct =
    series.episodesCount > 0 ? Math.round((progress / series.episodesCount) * 100) : 0

  return (
    <Link to={`/series/${series.id}`} className="group block text-left">
      <div className="aspect-[3/4] overflow-hidden rounded-xl bg-gradient-to-br from-indigo-500 via-purple-600 to-rose-500 shadow-sm" />
      <div className="mt-2">
        <p className="font-semibold leading-tight group-hover:underline">{series.title}</p>
        <p className="text-xs text-slate-500">
          {series.seasonsCount} seasons · {series.episodesCount} episodes
        </p>
        <div className="mt-1.5 flex items-center justify-between text-xs text-slate-500">
          <span>{progress > 0 ? `${progress} / ${series.episodesCount}` : 'Not started'}</span>
        </div>
        <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            className="h-full rounded-full bg-emerald-500"
            style={{ width: `${pct}%` }}
          />
        </div>
      </div>
    </Link>
  )
}
