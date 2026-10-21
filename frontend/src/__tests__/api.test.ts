import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchDisruptionOptions } from '../api'
import { apiResponse } from './fixtures'

describe('fetchDisruptionOptions', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns the parsed response of the backend endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(apiResponse()), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const data = await fetchDisruptionOptions()

    expect(fetchMock).toHaveBeenCalledWith('/api/disruption-options')
    expect(Object.keys(data.recommendations)).toHaveLength(48)
    expect(data.recommendations.AL0SAU.transport?.offerId).toBe('EXT-3')
  })

  it('fails with the problem detail sent by the backend', async () => {
    const problem = {
      status: 503,
      title: 'Disruption data unavailable',
      detail: 'The disruption could not be loaded. Please try again later.',
      code: 'DISRUPTION_DATA_UNAVAILABLE',
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(problem), { status: 503 })))

    await expect(fetchDisruptionOptions()).rejects.toThrow(
      'The disruption could not be loaded. Please try again later. (status 503)',
    )
  })

  it('falls back to the status code when the error has no problem body', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html>Bad Gateway</html>', { status: 502 })))

    await expect(fetchDisruptionOptions()).rejects.toThrow('Request failed (status 502)')
  })

  it('falls back to the status code when the problem has no detail', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ status: 404 }), { status: 404 })))

    await expect(fetchDisruptionOptions()).rejects.toThrow('Request failed (status 404)')
  })

  it('fails when the backend cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(fetchDisruptionOptions()).rejects.toThrow('Failed to fetch')
  })

  it('fails when the response is not JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html>proxy error</html>', { status: 200 })))

    await expect(fetchDisruptionOptions()).rejects.toThrow(SyntaxError)
  })
})
