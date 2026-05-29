import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { listSeries, listSeriesEpisodes, type Episode, type SeriesSummary } from '../api/catalog'
import { getProgress, updateProgress } from '../api/progress'
import { AppHeader } from '../components/AppHeader'
import { EpisodeRow } from '../components/EpisodeRow'
import { ProgressBanner } from '../components/ProgressBanner'
import { SeasonTabs } from '../components/SeasonTabs'
import { useAuth } from '../context'

export function SeriesDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { token } = useAuth()
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
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Link to="/" className="text-sm text-slate-500 hover:text-slate-900">
          ← Back to shows
        </Link>

        {status === 'loading' && <p className="mt-6 text-slate-500">Loading…</p>}
        {status === 'error' && (
          <p className="mt-6 text-rose-600">Couldn't load this series. Please try again.</p>
        )}

        {status === 'ready' && (
          <>
            <div className="mt-3">
              <h1 className="text-2xl font-bold">{series?.title ?? 'Series'}</h1>
              {series && (
                <p className="text-slate-500">
                  {series.seasonsCount} seasons · {series.episodesCount} episodes
                </p>
              )}
            </div>

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

            <div className="mt-10 rounded-xl border border-dashed border-slate-300 bg-slate-100/60 p-5 text-center">
              <p className="font-medium text-slate-600">Ask a spoiler-safe question</p>
              <p className="mt-1 text-sm text-slate-400">
                Coming next — the chat answers only from episodes up to your progress.
              </p>
              <button
                type="button"
                disabled
                className="mt-3 cursor-not-allowed rounded-md bg-slate-300 px-4 py-2 text-sm font-medium text-white"
              >
                Chat (coming soon)
              </button>
            </div>
          </>
        )}
      </main>
    </div>
  )
}
