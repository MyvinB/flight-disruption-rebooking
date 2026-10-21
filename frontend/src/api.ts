// Types mirror the JSON returned by GET /api/disruption-options

export type TransportMode = 'INTERNAL_FLIGHT' | 'EXTERNAL_FLIGHT' | 'TRAIN'

export interface Leg {
  operatorName: string
  operatorCode: string
  from: string
  to: string
  number: string
}

export interface TransportOption {
  offerId: string
  source: TransportMode
  operator: string
  origin: string
  destination: string
  departure: string // ISO-8601 with the airport's local offset
  arrival: string
  totalSeats: number
  suggestedPassengers: number // informational: passengers this offer is recommended to
  priceEur: number
  legs: Leg[]
}

export type SegmentStatus = 'CANCELLED' | 'WILL_BE_MISSED' | 'NOT_DEPARTED' | string

export interface Segment {
  flight: string
  from: string
  to: string
  departure: string
  arrival: string
  status: SegmentStatus
}

export interface Journey {
  origin: string
  destination: string
  segments: Segment[]
}

export interface Recommendation {
  passengers: number
  journey: Journey // original routing of the booking
  transport: TransportOption | null
}

export interface Disruption {
  flight: string
  origin: string
  destination: string
  scheduledDeparture: string
  scheduledArrival: string
  status: string
  reason: string
  affectedBookings: number
  affectedPassengers: number
}

export interface DisruptionOptionsResponse {
  disruption: Disruption
  transportOptions: Record<string, TransportOption[]> // keyed by "CGN#BER"
  recommendations: Record<string, Recommendation> // keyed by bookingRef
}

/** RFC 7807 error body returned by the backend (application/problem+json). */
export interface Problem {
  status: number
  title?: string
  detail?: string
  code?: string
}

export async function fetchDisruptionOptions(): Promise<DisruptionOptionsResponse> {
  const response = await fetch('/api/disruption-options')
  if (!response.ok) {
    const problem = await readProblem(response)
    throw new Error(`${problem?.detail ?? 'Request failed'} (status ${response.status})`)
  }
  return response.json()
}

/** The problem body if there is one; proxies and gateways may answer with HTML or nothing. */
async function readProblem(response: Response): Promise<Problem | null> {
  try {
    return (await response.json()) as Problem
  } catch {
    return null
  }
}
