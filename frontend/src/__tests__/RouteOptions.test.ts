import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import RouteOptions from '../components/RouteOptions.vue'
import { apiResponse } from './fixtures'

const mountOptions = (transportOptions = apiResponse().transportOptions) =>
  mount(RouteOptions, { props: { transportOptions } })

describe('RouteOptions', () => {
  it('shows one ranked table per searched route', () => {
    const routes = mountOptions().findAll('[data-test="route"]')

    expect(routes.map((r) => r.find('h3').text())).toEqual([
      'CGN → BER (8 options)',
      'CGN → DXB (5 options)',
      'PMI → BER (4 options)',
    ])
  })

  it('lists options in ranked order with source, local times, price and seats', () => {
    const dubai = mountOptions().findAll('[data-test="route"]')[1]
    const rows = dubai.findAll('tbody tr')

    expect(rows).toHaveLength(5)
    const first = rows[0].findAll('td').map((td) => td.text())
    expect(first[0]).toContain('Meridian Air')
    expect(first[0]).toContain('1 stop · VIE')
    expect(first.slice(1)).toEqual(['Other airline', '21 Jul 19:55', '22 Jul 06:05', '€424.00', '9', '30'])
    expect(rows[1].text()).toContain('Lufthansa')
  })

  it('highlights options recommended to more passengers than they have seats', () => {
    const dubai = mountOptions().findAll('[data-test="route"]')[1]
    const recommendedCells = dubai.findAll('tbody tr').map((r) => r.findAll('td')[6])

    // EXT-3: 30 passengers for 9 seats
    expect(recommendedCells[0].classes()).toContain('warning')
    // INT-5: recommended to nobody
    expect(recommendedCells[1].text()).toBe('0')
    expect(recommendedCells[1].classes()).not.toContain('warning')
  })

  it('does not highlight an option recommended to exactly as many passengers as it has seats', () => {
    const data = apiResponse().transportOptions
    data['CGN#DXB'][0].suggestedPassengers = data['CGN#DXB'][0].totalSeats

    const cell = mountOptions(data).findAll('[data-test="route"]')[1].find('tbody tr').findAll('td')[6]
    expect(cell.classes()).not.toContain('warning')
  })

  it('says so when no source returned an option for a route', () => {
    const data = apiResponse().transportOptions
    data['PMI#BER'] = []

    const palma = mountOptions(data).findAll('[data-test="route"]')[2]
    expect(palma.text()).toContain('(0 options)')
    expect(palma.text()).toContain('No source returned an option for this route.')
    expect(palma.find('table').exists()).toBe(false)
  })

  it('renders nothing but the heading when no route was searched', () => {
    const wrapper = mountOptions({})

    expect(wrapper.findAll('[data-test="route"]')).toHaveLength(0)
    expect(wrapper.find('h2').text()).toBe('All options per route')
  })
})
