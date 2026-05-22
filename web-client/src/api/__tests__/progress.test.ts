import { afterEach, describe, expect, it, vi } from 'vitest'
import { getProgress, updateProgress } from '../progress'

function okJson(body: unknown) {
  return { ok: true, status: 200, statusText: '', json: async () => body } as Response
}

afterEach(() => vi.unstubAllGlobals())

describe('progress api', () => {
  it('getProgress sends the bearer token to the user-progress base', async () => {
    const fetchMock = vi.fn().mockResolvedValue(okJson([]))
    vi.stubGlobal('fetch', fetchMock)

    await getProgress('tok-1')

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/user-progress/progress')
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer tok-1')
  })

  it('updateProgress PUTs the body with the token', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(okJson({ seriesId: 's1', episodeIndex: 5, updatedAt: 'now' }))
    vi.stubGlobal('fetch', fetchMock)

    await updateProgress('tok-1', { seriesId: 's1', episodeIndex: 5 })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/user-progress/progress')
    expect(options.method).toBe('PUT')
    expect(JSON.parse(options.body)).toEqual({ seriesId: 's1', episodeIndex: 5 })
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer tok-1')
  })
})
