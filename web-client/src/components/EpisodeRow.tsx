import type { Episode } from '../api/catalog'

export function EpisodeRow({
  episode,
  watched,
  isCurrent,
  onSetCurrent,
}: {
  episode: Episode
  watched: boolean
  isCurrent: boolean
  onSetCurrent: (episodeIndex: number) => void
}) {
  const ring = isCurrent ? 'ring-2 ring-emerald-500' : 'border border-slate-200'

  return (
    <div className={`flex items-start gap-3 rounded-lg ${ring} bg-white p-4`} data-testid="episode-row">
      <div className="w-6 pt-0.5 text-center" aria-hidden>
        {watched ? (
          <span className="text-emerald-500" data-testid="watched-indicator">
            ✓
          </span>
        ) : (
          <span className="text-slate-300">🔒</span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-3">
          <p className="font-medium" data-testid="episode-title">
            E{episode.episodeNumber}. {episode.title}
          </p>
          {isCurrent ? (
            <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
              You're here
            </span>
          ) : (
            <button
              type="button"
              onClick={() => onSetCurrent(episode.episodeIndex)}
              className="shrink-0 rounded-md border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-100"
              data-testid="watch-button"
            >
              Set as current
            </button>
          )}
        </div>
        <p className={`mt-1 text-sm text-slate-600${watched ? '' : ' spoiler'}`}>
          {episode.summary}
        </p>
        {!watched && (
          <p className="mt-1 text-xs font-medium text-slate-400">
            Hidden to avoid spoilers — watch to unlock
          </p>
        )}
      </div>
    </div>
  )
}
