import { describe, expect, it } from 'vitest'
import {
  formatDelay,
  formatLocalTime,
  formatMode,
  formatPrice,
  formatRoute,
  formatSegmentStatus,
  formatStops,
  isDirect,
  isOverDemanded,
} from '../format'
import type { TransportOption } from '../api'

describe('formatLocalTime', () => {
  it('keeps the airport local time instead of converting to the browser time zone', () => {
    expect(formatLocalTime('2026-07-22T06:05:00+04:00')).toBe('22 Jul 06:05')
    expect(formatLocalTime('2026-07-21T21:50:00+02:00')).toBe('21 Jul 21:50')
  })

  it('does not depend on the offset being present', () => {
    expect(formatLocalTime('2026-12-01T09:05')).toBe('1 Dec 09:05')
  })

  it('returns input it cannot parse unchanged', () => {
    expect(formatLocalTime('')).toBe('')
    expect(formatLocalTime('21.07.2026 19:55')).toBe('21.07.2026 19:55')
    expect(formatLocalTime('2026-07-21')).toBe('2026-07-21')
  })
})

describe('formatPrice', () => {
  it('always shows two decimals', () => {
    expect(formatPrice(189)).toBe('€189.00')
    expect(formatPrice(89.9)).toBe('€89.90')
    expect(formatPrice(0)).toBe('€0.00')
  })

  it('rounds to cents', () => {
    expect(formatPrice(99.999)).toBe('€100.00')
  })
})

describe('formatStops', () => {
  const leg = (from: string, to: string) => ({ operatorName: 'X', operatorCode: 'XX', from, to, number: '1' })

  it('names the connection points of multi-leg itineraries', () => {
    expect(formatStops([leg('CGN', 'BER')])).toBe('Direct')
    expect(formatStops([leg('CGN', 'VIE'), leg('VIE', 'DXB')])).toBe('1 stop · VIE')
    expect(formatStops([leg('CGN', 'FRA'), leg('FRA', 'MUC'), leg('MUC', 'BER')])).toBe('2 stops · FRA, MUC')
  })

  it('treats an option without legs as direct', () => {
    expect(isDirect([])).toBe(true)
    expect(formatStops([])).toBe('Direct')
    expect(isDirect([leg('CGN', 'VIE'), leg('VIE', 'DXB')])).toBe(false)
  })
})

describe('isOverDemanded', () => {
  it('flags options recommended to more passengers than they have seats', () => {
    const option = { totalSeats: 24, suggestedPassengers: 60 } as TransportOption
    expect(isOverDemanded(option)).toBe(true)
    expect(isOverDemanded({ ...option, suggestedPassengers: 24 })).toBe(false)
  })
})

describe('formatDelay', () => {
  it('compares arrival instants across time zones', () => {
    expect(formatDelay('2026-07-21T19:45:00+02:00', '2026-07-21T23:05:00+02:00')).toBe('+3h 20m later')
    // Original EW 2590 lands 06:20 Dubai time; EXT-3 lands 06:05 Dubai time
    expect(formatDelay('2026-07-22T06:20:00+04:00', '2026-07-22T06:05:00+04:00')).toBe('15m earlier')
    expect(formatDelay('2026-07-22T06:20:00+04:00', '2026-07-22T04:20:00+02:00')).toBe('on time')
  })

  it('formats whole hours, minutes only and arrivals on the next day', () => {
    expect(formatDelay('2026-07-21T19:45:00+02:00', '2026-07-21T20:45:00+02:00')).toBe('+1h 00m later')
    expect(formatDelay('2026-07-21T19:45:00+02:00', '2026-07-21T20:30:00+02:00')).toBe('+45m later')
    // TRN-3 arrives the next morning
    expect(formatDelay('2026-07-21T19:45:00+02:00', '2026-07-22T10:05:00+02:00')).toBe('+14h 20m later')
    expect(formatDelay('2026-07-21T19:45:00+02:00', '2026-07-21T17:45:00+02:00')).toBe('2h 00m earlier')
  })
})

describe('formatRoute', () => {
  it('turns route keys into arrows', () => {
    expect(formatRoute('CGN#DXB')).toBe('CGN → DXB')
    expect(formatRoute('CGN')).toBe('CGN')
  })
})

describe('formatMode', () => {
  it('names every source', () => {
    expect(formatMode('INTERNAL_FLIGHT')).toBe('Lufthansa Group')
    expect(formatMode('EXTERNAL_FLIGHT')).toBe('Other airline')
    expect(formatMode('TRAIN')).toBe('Train')
  })
})

describe('formatSegmentStatus', () => {
  it('uses readable labels for known statuses', () => {
    expect(formatSegmentStatus('CANCELLED')).toBe('cancelled')
    expect(formatSegmentStatus('WILL_BE_MISSED')).toBe('will be missed')
    expect(formatSegmentStatus('NOT_DEPARTED')).toBe('not departed')
  })

  it('still shows statuses it does not know', () => {
    expect(formatSegmentStatus('DIVERTED_TO_DUS')).toBe('diverted to dus')
  })
})
