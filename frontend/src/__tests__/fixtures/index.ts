import type { DisruptionOptionsResponse } from '../../api'
import response from './disruption-options.json'

/** Real GET /api/disruption-options response for the mock data; a fresh copy per call so tests can change it. */
export function apiResponse(): DisruptionOptionsResponse {
  return structuredClone(response) as DisruptionOptionsResponse
}
