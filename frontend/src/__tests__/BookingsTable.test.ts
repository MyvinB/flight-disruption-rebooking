import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BookingsTable from '../components/BookingsTable.vue'
import type { Journey, Recommendation, TransportOption } from '../api'

const ext1: TransportOption = {
  offerId: 'EXT-1',
  source: 'EXTERNAL_FLIGHT',
  operator: 'Nimbus Airways',
  origin: 'CGN',
  destination: 'BER',
  departure: '2026-07-21T20:40:00+02:00',
  arrival: '2026-07-21T21:50:00+02:00',
  totalSeats: 24,
  suggestedPassengers: 60,
  priceEur: 189,
  legs: [{ operatorName: 'Nimbus Airways', operatorCode: 'NB', from: 'CGN', to: 'BER', number: '88' }],
}

const int5: TransportOption = {
  ...ext1,
  offerId: 'INT-5',
  source: 'INTERNAL_FLIGHT',
  operator: 'Lufthansa',
  destination: 'DXB',
  totalSeats: 20,
  suggestedPassengers: 4,
  legs: [
    { operatorName: 'Lufthansa', operatorCode: 'LH', from: 'CGN', to: 'FRA', number: '1975' },
    { operatorName: 'Lufthansa', operatorCode: 'LH', from: 'FRA', to: 'DXB', number: '630' },
  ],
}

const ew4711 = {
  flight: 'EW 4711',
  from: 'CGN',
  to: 'BER',
  departure: '2026-07-21T18:35:00+02:00',
  arrival: '2026-07-21T19:45:00+02:00',
  status: 'CANCELLED',
}

const toBerlin: Journey = { origin: 'CGN', destination: 'BER', segments: [ew4711] }

const toDubai: Journey = {
  origin: 'CGN',
  destination: 'DXB',
  segments: [
    ew4711,
    {
      flight: 'EW 2590',
      from: 'BER',
      to: 'DXB',
      departure: '2026-07-21T21:40:00+02:00',
      arrival: '2026-07-22T06:20:00+04:00',
      status: 'WILL_BE_MISSED',
    },
  ],
}

const recommendations: Record<string, Recommendation> = {
  D3HRVC: { passengers: 2, journey: toBerlin, transport: ext1 },
  AL0SAU: { passengers: 4, journey: toDubai, transport: int5 },
  NOSEAT: { passengers: 4, journey: toDubai, transport: null },
}

describe('BookingsTable', () => {
  const mountTable = () =>
    mount(BookingsTable, { props: { recommendations, routes: ['CGN#BER', 'CGN#DXB'] } })

  it('shows one row per booking with its recommended alternative', () => {
    const rows = mountTable().findAll('[data-test="booking-row"]')

    expect(rows).toHaveLength(3)
    expect(rows[0].text()).toContain('D3HRVC')
    expect(rows[0].text()).toContain('Nimbus Airways')
    expect(rows[0].text()).toContain('high demand')
    expect(rows[1].text()).not.toContain('high demand')
    expect(rows[2].text()).toContain('No alternative')
  })

  it('marks direct and multi-leg itineraries and lists every leg', () => {
    const rows = mountTable().findAll('[data-test="booking-row"]')

    expect(rows[0].find('[data-test="stops"]').text()).toBe('Direct')
    expect(rows[1].find('[data-test="stops"]').text()).toBe('1 stop · FRA')
    const legs = rows[1].findAll('[data-test="legs"] li').map((li) => li.text())
    expect(legs).toEqual([expect.stringContaining('LH 1975'), expect.stringContaining('LH 630')])
    expect(legs[0]).toContain('CGN → FRA')
    expect(legs[1]).toContain('FRA → DXB')
  })

  it('shows the original journey with segment statuses and the change in arrival time', () => {
    const rows = mountTable().findAll('[data-test="booking-row"]')

    const statuses = rows[1].findAll('[data-test="segment-status"]').map((s) => s.text())
    expect(statuses).toEqual(['cancelled', 'will be missed'])
    expect(rows[1].find('[data-test="segments"]').text()).toContain('EW 2590')
    // Original arrival 19:45, alternative 21:50
    expect(rows[0].find('[data-test="delay"]').text()).toBe('+2h 05m later')
  })

  it('filters bookings by their original route, including bookings without an alternative', async () => {
    const wrapper = mountTable()
    await wrapper.find('[data-test="route-filter"]').setValue('CGN#DXB')

    const rows = wrapper.findAll('[data-test="booking-row"]')
    expect(rows.map((r) => r.find('.mono').text())).toEqual(['AL0SAU', 'NOSEAT'])
  })
})
