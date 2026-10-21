import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DisruptionHeader from '../components/DisruptionHeader.vue'
import { apiResponse } from './fixtures'

describe('DisruptionHeader', () => {
  it('summarizes the cancelled flight and how many passengers it affects', () => {
    const text = mount(DisruptionHeader, { props: { disruption: apiResponse().disruption } }).text()

    expect(text).toContain('EW 4711 · CGN → BER')
    expect(text).toContain('Scheduled 21 Jul 18:35 – 21 Jul 19:45')
    expect(text).toContain('CANCELLED')
    expect(text).toContain('Reason: technical')
    expect(text).toContain('48 bookings · 120 passengers')
  })

  it('shows an unparseable schedule as given instead of failing', () => {
    const disruption = { ...apiResponse().disruption, scheduledDeparture: 'unknown' }

    expect(mount(DisruptionHeader, { props: { disruption } }).text()).toContain('Scheduled unknown –')
  })
})
