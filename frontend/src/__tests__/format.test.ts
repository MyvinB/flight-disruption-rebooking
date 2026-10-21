import { describe, expect, it } from 'vitest'
import { formatDelay, formatLocalTime, formatPrice, formatStops, isOverDemanded } from '../format'
import type { TransportOption } from '../api'

describe('formatLocalTime', () => {
  it('keeps the airport local time instead of converting to the browser time zone', () => {
    expect(formatLocalTime('2026-07-22T06:05:00+04:00')).toBe('22 Jul 06:05')
    expect(formatLocalTime('2026-07-21T21:50:00+02:00')).toBe('21 Jul 21:50')
  })
})

describe('formatPrice', () => {
  it('always shows two decimals', () => {
    expect(formatPrice(189)).toBe('€189.00')
    expect(formatPrice(89.9)).toBe('€89.90')
  })
})

describe('formatStops', () => {
  const leg = (from: string, to: string) => ({ operatorName: 'X', operatorCode: 'XX', from, to, number: '1' })

  it('names the connection points of multi-leg itineraries', () => {
    expect(formatStops([leg('CGN', 'BER')])).toBe('Direct')
    expect(formatStops([leg('CGN', 'VIE'), leg('VIE', 'DXB')])).toBe('1 stop · VIE')
    expect(formatStops([leg('CGN', 'FRA'), leg('FRA', 'MUC'), leg('MUC', 'BER')])).toBe('2 stops · FRA, MUC')
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
})
