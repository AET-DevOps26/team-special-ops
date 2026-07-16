import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { listSeries, listSeriesEpisodes, type Episode, type SeriesSummary } from '../api/catalog'
import { getProgress, updateProgress } from '../api/progress'
import { AppHeader } from '../components/AppHeader'
import { ChatPanel } from '../components/ChatPanel'
import { EpisodeRow } from '../components/EpisodeRow'
import { ProgressBanner } from '../components/ProgressBanner'
import { SeasonTabs } from '../components/SeasonTabs'
import { useAuth } from '../context'
import { useLikes } from '../hooks/useLikes'

export function SeriesDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { token } = useAuth()
  const { likedIds, toggleLike } = useLikes(token)
  const liked = id ? likedIds.has(id) : false
  const [series, setSeries] = useState<SeriesSummary | null>(null)
  const [episodes, setEpisodes] = useState<Episode[]>([])
  const [progress, setProgress] = useState(0)
  const [selectedSeason, setSelectedSeason] = useState<number | null>(null)
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')

  useEffect(() => {
    if (!id || !token) return
    let cancelled = false
    setStatus('loading')

    Promise.all([listSeries(), listSeriesEpisodes(id), getProgress(token)])
      .then(([seriesList, eps, progressList]) => {
        if (cancelled) return
        const prog = progressList.find((p) => p.seriesId === id)?.episodeIndex ?? 0
        const seasons = [...new Set(eps.map((e) => e.season))].sort((a, b) => a - b)
        const seasonOnLoad = eps.find((e) => e.episodeIndex === prog)?.season ?? seasons[0] ?? null
        setSeries(seriesList.find((s) => s.id === id) ?? null)
        setEpisodes(eps)
        setProgress(prog)
        setSelectedSeason(seasonOnLoad)
        setStatus('ready')
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [id, token])

  async function handleSetCurrent(episodeIndex: number) {
    if (!id || !token) return
    const saved = await updateProgress(token, { seriesId: id, episodeIndex })
    setProgress(saved.episodeIndex)
  }

  const seasons = [...new Set(episodes.map((e) => e.season))].sort((a, b) => a - b)
  const currentSeason = episodes.find((e) => e.episodeIndex === progress)?.season ?? null
  const shownEpisodes = episodes.filter((e) => e.season === selectedSeason)

  return (
    <div className="min-h-screen bg-slate-50">
      <AppHeader />
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Link to="/" className="text-sm text-slate-500 hover:text-slate-900">
          ← Back to shows
        </Link>

        {status === 'loading' && <p className="mt-6 text-slate-500">Loading…</p>}
        {status === 'error' && (
          <p className="mt-6 text-rose-600">Couldn't load this series. Please try again.</p>
        )}

        {status === 'ready' && id && token && (
          <>
            <div className="mt-3 flex items-start justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold">{series?.title ?? 'Series'}</h1>
                {series && (
                  <p className="text-slate-500">
                    {series.seasonsCount} seasons · {series.episodesCount} episodes
                  </p>
                )}
              </div>
              <button
                type="button"
                aria-label={liked ? 'Unlike' : 'Like'}
                aria-pressed={liked}
                data-testid="like-button"
                onClick={() => toggleLike(id)}
                className={`flex shrink-0 items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-medium transition ${
                  liked
                    ? 'border-rose-200 bg-rose-50 text-rose-600 hover:bg-rose-100'
                    : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                }`}
              >
                <span>{liked ? '♥' : '♡'}</span>
                {liked ? 'Liked' : 'Like'}
              </button>
            </div>

            <div className="mt-6 grid gap-8 lg:grid-cols-[1fr_20rem] lg:items-start">
              <div>
                <ProgressBanner episodes={episodes} progress={progress} />

                <SeasonTabs
                  seasons={seasons}
                  selected={selectedSeason}
                  current={currentSeason}
                  onSelect={setSelectedSeason}
                />

                <div className="mt-4 space-y-2">
                  {shownEpisodes.map((ep) => (
                    <EpisodeRow
                      key={ep.id}
                      episode={ep}
                      watched={ep.episodeIndex <= progress}
                      isCurrent={ep.episodeIndex === progress}
                      onSetCurrent={handleSetCurrent}
                    />
                  ))}
                </div>
              </div>

              <div className="lg:sticky lg:top-6">
                <ChatPanel
                  token={token}
                  seriesId={id}
                  progress={progress}
                  episodes={episodes}
                />
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  )
}
