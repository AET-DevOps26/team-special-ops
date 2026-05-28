import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { listSeries, listSeriesEpisodes, type Episode, type SeriesSummary } from '../api/catalog'
import { getProgress, updateProgress } from '../api/progress'
import { AppHeader } from '../components/AppHeader'
import { EpisodeRow } from '../components/EpisodeRow'
import { ProgressBanner } from '../components/ProgressBanner'
import { useAuth } from '../context'

function groupBySeason(episodes: Episode[]): [number, Episode[]][] {
  const map = new Map<number, Episode[]>()
  for (const ep of episodes) {
    const list = map.get(ep.season) ?? []
    list.push(ep)
    map.set(ep.season, list)
  }
  return [...map.entries()].sort((a, b) => a[0] - b[0])
}

export function SeriesDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { token } = useAuth()
  const [series, setSeries] = useState<SeriesSummary | null>(null)
  const [episodes, setEpisodes] = useState<Episode[]>([])
  const [progress, setProgress] = useState(0)
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')

  useEffect(() => {
    if (!id || !token) return
    let cancelled = false
    setStatus('loading')

    Promise.all([listSeries(), listSeriesEpisodes(id), getProgress(token)])
      .then(([seriesList, eps, progressList]) => {
        if (cancelled) return
        setSeries(seriesList.find((s) => s.id === id) ?? null)
        setEpisodes(eps)
        setProgress(progressList.find((p) => p.seriesId === id)?.episodeIndex ?? 0)
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

            <div className="mt-6 space-y-8">
              {groupBySeason(episodes).map(([season, eps]) => (
                <div key={season}>
                  <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-400">
                    Season {season}
                  </h2>
                  <div className="space-y-2">
                    {eps.map((ep) => (
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
