import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

vi.mock('../../api/likes', () => ({
  getLikes: vi.fn(),
  likeSeries: vi.fn(),
  unlikeSeries: vi.fn(),
}))

import { getLikes, likeSeries, unlikeSeries } from '../../api/likes'
import { useLikes } from '../useLikes'

const mockGetLikes = vi.mocked(getLikes)
const mockLike = vi.mocked(likeSeries)
const mockUnlike = vi.mocked(unlikeSeries)

afterEach(() => vi.clearAllMocks())

describe('useLikes', () => {
  it('loads liked ids in the order the API returns them (newest first)', async () => {
    mockGetLikes.mockResolvedValue(['a', 'b'])
    const { result } = renderHook(() => useLikes('tok'))

    await waitFor(() => expect(result.current.status).toBe('ready'))
    expect(result.current.orderedLikedIds).toEqual(['a', 'b'])
    expect(result.current.likedIds.has('a')).toBe(true)
  })

  it('surfaces an error status when the load fails', async () => {
    mockGetLikes.mockRejectedValue(new Error('down'))
    const { result } = renderHook(() => useLikes('tok'))

    await waitFor(() => expect(result.current.status).toBe('error'))
  })

  it('optimistically prepends a new like and calls the API', async () => {
    mockGetLikes.mockResolvedValue(['a'])
    mockLike.mockResolvedValue(undefined)
    const { result } = renderHook(() => useLikes('tok'))
    await waitFor(() => expect(result.current.status).toBe('ready'))

    act(() => result.current.toggleLike('c'))

    expect(result.current.orderedLikedIds).toEqual(['c', 'a'])
    expect(mockLike).toHaveBeenCalledWith('tok', 'c')
  })

  it('optimistically removes an unlike', async () => {
    mockGetLikes.mockResolvedValue(['a', 'b'])
    mockUnlike.mockResolvedValue(undefined)
    const { result } = renderHook(() => useLikes('tok'))
    await waitFor(() => expect(result.current.status).toBe('ready'))

    act(() => result.current.toggleLike('a'))

    expect(result.current.orderedLikedIds).toEqual(['b'])
    expect(mockUnlike).toHaveBeenCalledWith('tok', 'a')
  })

  it('reverts a failed toggle without clobbering a concurrent toggle of another series', async () => {
    mockGetLikes.mockResolvedValue([])
    let rejectA: (e: unknown) => void = () => {}
    // Request for 'A' hangs (we reject it later); request for 'B' succeeds immediately.
    mockLike.mockImplementation((_t, id) =>
      id === 'A' ? new Promise<void>((_, r) => (rejectA = r)) : Promise.resolve(),
    )
    const { result } = renderHook(() => useLikes('tok'))
    await waitFor(() => expect(result.current.status).toBe('ready'))

    act(() => result.current.toggleLike('A')) // optimistic ['A'], request A pending
    act(() => result.current.toggleLike('B')) // optimistic ['B','A'], request B resolves
    expect(result.current.orderedLikedIds).toEqual(['B', 'A'])

    await act(async () => {
      rejectA(new Error('A failed'))
    })
    // A reverts out; B — which succeeded — must survive.
    await waitFor(() => expect(result.current.orderedLikedIds).toEqual(['B']))
  })

  it('ignores repeat toggles for a series while its request is in flight', async () => {
    mockGetLikes.mockResolvedValue([])
    mockLike.mockReturnValue(new Promise<void>(() => {})) // never settles
    const { result } = renderHook(() => useLikes('tok'))
    await waitFor(() => expect(result.current.status).toBe('ready'))

    act(() => result.current.toggleLike('c'))
    act(() => result.current.toggleLike('c')) // dropped by the in-flight guard

    expect(mockLike).toHaveBeenCalledTimes(1)
    expect(mockUnlike).not.toHaveBeenCalled()
  })
})
