import type { Episode } from '../api/catalog'

export function ProgressBanner({
  episodes,
  progress,
}: {
  episodes: Episode[]
  progress: number
}) {
  const total = episodes.length
  const current = episodes.find((e) => e.episodeIndex === progress) ?? null
  const pct = total > 0 ? Math.round((progress / total) * 100) : 0

  return (
    <div className="mt-5 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
            Your progress
          </p>
          <p className="mt-0.5 text-lg font-semibold">
            {current
              ? `Season ${current.season}, Episode ${current.episodeNumber} — ${current.title}`
              : 'Not started'}
          </p>
        </div>
        <span className="text-sm text-slate-500">
          {progress} / {total} watched
        </span>
      </div>
      <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-slate-100">
        <div
          className="h-full rounded-full bg-emerald-500 transition-all"
          style={{ width: `${pct}%` }}
        />
      </div>
      <p className="mt-3 text-sm text-slate-500">
        Episodes and chat answers are limited to what you&apos;ve seen. Set a row below as your
        current point — you can move it forwards or backwards anytime.
      </p>
    </div>
  )
}
