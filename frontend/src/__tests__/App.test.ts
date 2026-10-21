import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import App from '../App.vue'
import { fetchDisruptionOptions } from '../api'
import { apiResponse } from './fixtures'

vi.mock('../api', () => ({ fetchDisruptionOptions: vi.fn() }))

const fetchMock = vi.mocked(fetchDisruptionOptions)

describe('App', () => {
  beforeEach(() => {
    fetchMock.mockReset()
  })

  it('shows a loading message while the sources are searched', async () => {
    fetchMock.mockReturnValue(new Promise(() => {}))

    const wrapper = mount(App)
    await nextTick()

    expect(wrapper.text()).toContain('Searching all sources…')
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('shows the disruption, every booking and every route once loaded', async () => {
    fetchMock.mockResolvedValue(apiResponse())

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('EW 4711 · CGN → BER')
    expect(wrapper.findAll('[data-test="booking-row"]')).toHaveLength(48)
    expect(wrapper.findAll('[data-test="route"]')).toHaveLength(3)
    const filterOptions = wrapper.findAll('[data-test="route-filter"] option').map((o) => o.text())
    expect(filterOptions).toEqual(['All routes', 'CGN → BER', 'CGN → DXB', 'PMI → BER'])
  })

  it('shows the error and recovers when trying again', async () => {
    fetchMock.mockRejectedValueOnce(new Error('Request failed with status 503'))
    fetchMock.mockResolvedValueOnce(apiResponse())

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('.error').text()).toContain('Could not load options: Request failed with status 503')
    expect(wrapper.findAll('[data-test="booking-row"]')).toHaveLength(0)

    await wrapper.find('.error button').trigger('click')
    await flushPromises()

    expect(wrapper.find('.error').exists()).toBe(false)
    expect(wrapper.findAll('[data-test="booking-row"]')).toHaveLength(48)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('shows non-Error failures as text', async () => {
    fetchMock.mockRejectedValue('offline')

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('.error').text()).toContain('Could not load options: offline')
  })

  it('keeps the last result visible when a refresh fails', async () => {
    fetchMock.mockResolvedValueOnce(apiResponse())
    fetchMock.mockRejectedValueOnce(new Error('timeout'))

    const wrapper = mount(App)
    await flushPromises()
    await wrapper.find('.toolbar button').trigger('click')
    await flushPromises()

    expect(wrapper.find('.error').text()).toContain('timeout')
    expect(wrapper.findAll('[data-test="booking-row"]')).toHaveLength(48)
  })
})
