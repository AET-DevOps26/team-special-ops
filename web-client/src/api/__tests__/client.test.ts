import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError, fetchJson } from '../client'

function mockResponse(body: unknown, init: { ok: boolean; status: number }) {
  return {
    ok: init.ok,
    status: init.status,
    statusText: '',
    json: async () => body,
  } as Response
}

afterEach(() => {
  vi.unstubAllGlobals()
})

// The base-url plumbing (default :8081 / catalog :8082) is covered end-to-end by
// progress.test.ts and catalog.test.ts, which exercise it through the real API
// functions. This file covers only fetchJson's own error-parsing behavior.
describe('fetchJson', () => {
  it('throws ApiRequestError with status, code, and message on non-2xx', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(mockResponse({ code: 'BAD', message: 'nope' }, { ok: false, status: 400 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(fetchJson('/x')).rejects.toMatchObject({
      name: 'ApiRequestError',
      status: 400,
      code: 'BAD',
      message: 'nope',
    })
    expect(ApiRequestError).toBeDefined()
  })
})
