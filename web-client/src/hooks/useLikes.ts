import { useEffect, useMemo, useRef, useState } from 'react'
import { getLikes, likeSeries, unlikeSeries } from '../api/likes'

type Status = 'loading' | 'ready' | 'error'

/**
 * Loads the current user's liked series (newest first) and exposes an optimistic toggle.
 *
 * - `orderedLikedIds` is the source of truth, kept newest-liked-first so "My Shows" can render in
 *   like order; `likedIds` is the derived Set for O(1) membership checks on cards.
 * - Toggles are optimistic and revert to the exact prior order on failure.
 * - A per-series in-flight guard drops repeat toggles until the outstanding request settles, so a
 *   fast double-tap can't desync the client from the server via reordered PUT/DELETE.
 */
export function useLikes(token: string | null) {
  const [orderedLikedIds, setOrderedLikedIds] = useState<string[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const inFlight = useRef<Set<string>>(new Set())

  useEffect(() => {
    if (!token) return
    let cancelled = false
    setStatus('loading')
    getLikes(token)
      .then((ids) => {
        if (cancelled) return
        setOrderedLikedIds(ids)
        setStatus('ready')
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })
    return () => {
      cancelled = true
    }
  }, [token])

  const likedIds = useMemo(() => new Set(orderedLikedIds), [orderedLikedIds])

  function toggleLike(seriesId: string) {
    if (!token) return
    if (inFlight.current.has(seriesId)) return // a request for this series is still settling

    const wasLiked = likedIds.has(seriesId)
    inFlight.current.add(seriesId)

    // Newly liked series go to the front (newest first); unlikes drop out. Both apply and revert
    // are functional updates that touch only this id, so a failed revert can't clobber a
    // concurrent toggle of another series.
    const add = (ids: string[]) => (ids.includes(seriesId) ? ids : [seriesId, ...ids])
    const remove = (ids: string[]) => ids.filter((id) => id !== seriesId)

    setOrderedLikedIds(wasLiked ? remove : add) // optimistic
    const request = wasLiked ? unlikeSeries(token, seriesId) : likeSeries(token, seriesId)
    request
      .catch(() => setOrderedLikedIds(wasLiked ? add : remove)) // revert: the inverse op
      .finally(() => inFlight.current.delete(seriesId))
  }

  return { likedIds, orderedLikedIds, status, toggleLike }
}
