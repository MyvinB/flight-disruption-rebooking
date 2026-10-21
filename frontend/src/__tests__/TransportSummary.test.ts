import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TransportSummary from '../components/TransportSummary.vue'
import { apiResponse } from './fixtures'

const ext3 = () => apiResponse().transportOptions['CGN#DXB'][0]

describe('TransportSummary', () => {
  it('lists every leg of a connection with flight number and airports', () => {
    const wrapper = mount(TransportSummary, { props: { transport: ext3() } })

    expect(wrapper.find('strong').text()).toBe('Meridian Air')
    expect(wrapper.find('[data-test="stops"]').classes()).toContain('badge-info')
    expect(wrapper.findAll('[data-test="legs"] li').map((li) => li.text())).toEqual([
      'MD 442CGN → VIE',
      'MD 86VIE → DXB',
    ])
  })

  it('marks direct options with a neutral badge', () => {
    const ext1 = apiResponse().transportOptions['CGN#BER'][0]

    const stops = mount(TransportSummary, { props: { transport: ext1 } }).find('[data-test="stops"]')
    expect(stops.text()).toBe('Direct')
    expect(stops.classes()).toContain('badge-neutral')
  })

  it('names the operating airline of legs flown by another carrier', () => {
    const transport = ext3()
    transport.legs[1] = { ...transport.legs[1], operatorName: 'Partner Air' }

    const legs = mount(TransportSummary, { props: { transport } }).findAll('[data-test="legs"] li')
    expect(legs[0].text()).not.toContain('(')
    expect(legs[1].text()).toContain('(Partner Air)')
  })
})
