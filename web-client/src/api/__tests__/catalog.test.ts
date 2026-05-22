import { afterEach, describe, expect, it, vi } from 'vitest'
import { listSeries, listSeriesEpisodes } from '../catalog'

function okJson(body: unknown) {
  return { ok: true, status: 200, statusText: '', json: async () => body } as Response
}

afterEach(() => vi.unstubAllGlobals())

describe('catalog api', () => {
  it('listSeries calls the catalog base, no auth header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(okJson([]))
    vi.stubGlobal('fetch', fetchMock)

    await listSeries()

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8082/catalog/series')
    const headers = new Headers(options.headers)
    expect(headers.has('Authorization')).toBe(false)
  })

  it('listSeriesEpisodes targets the series id path', async () => {
    const fetchMock = vi.fn().mockResolvedValue(okJson([]))
    vi.stubGlobal('fetch', fetchMock)

    await listSeriesEpisodes('abc-123')

    expect(fetchMock.mock.calls[0][0]).toBe(
      'http://localhost:8082/catalog/series/abc-123/episodes',
    )
  })
})
