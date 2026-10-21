import type { Leg, TransportMode, TransportOption } from './api'

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/**
 * Formats an ISO date-time in the airport's own local time, e.g. "22 Jul 06:05".
 * The string is read as-is instead of via Date, which would convert it to the browser's time zone.
 */
export function formatLocalTime(iso: string): string {
  const [, month, day, time] = /^\d{4}-(\d{2})-(\d{2})T(\d{2}:\d{2})/.exec(iso) ?? []
  if (!time) {
    return iso
  }
  return `${Number(day)} ${MONTHS[Number(month) - 1]} ${time}`
}

export function formatPrice(eur: number): string {
  return `€${eur.toFixed(2)}`
}

export function isDirect(legs: Leg[]): boolean {
  return legs.length <= 1
}

/** "Direct", "1 stop · VIE", "2 stops · FRA, MUC" — stops are the arrival points of every leg but the last. */
export function formatStops(legs: Leg[]): string {
  if (isDirect(legs)) {
    return 'Direct'
  }
  const stops = legs.slice(0, -1).map((l) => l.to)
  return `${stops.length} ${stops.length === 1 ? 'stop' : 'stops'} · ${stops.join(', ')}`
}

export function formatRoute(routeKey: string): string {
  return routeKey.replace('#', ' → ')
}

const MODE_LABELS: Record<TransportMode, string> = {
  INTERNAL_FLIGHT: 'Lufthansa Group',
  EXTERNAL_FLIGHT: 'Other airline',
  TRAIN: 'Train',
}

export function formatMode(mode: TransportMode): string {
  return MODE_LABELS[mode]
}

/** More passengers are recommended this offer than it has seats. */
export function isOverDemanded(option: TransportOption): boolean {
  return option.suggestedPassengers > option.totalSeats
}

/**
 * How much later (or earlier) the alternative arrives than originally planned, e.g. "+3h 20m later".
 * Compares absolute instants, so different airport offsets are handled.
 */
export function formatDelay(originalArrival: string, newArrival: string): string {
  const minutes = Math.round((Date.parse(newArrival) - Date.parse(originalArrival)) / 60000)
  if (minutes === 0) {
    return 'on time'
  }
  const abs = Math.abs(minutes)
  const text = abs >= 60 ? `${Math.floor(abs / 60)}h ${String(abs % 60).padStart(2, '0')}m` : `${abs}m`
  return minutes > 0 ? `+${text} later` : `${text} earlier`
}

const STATUS_LABELS: Record<string, string> = {
  CANCELLED: 'cancelled',
  WILL_BE_MISSED: 'will be missed',
  NOT_DEPARTED: 'not departed',
}

export function formatSegmentStatus(status: string): string {
  return STATUS_LABELS[status] ?? status.toLowerCase().replace(/_/g, ' ')
}
