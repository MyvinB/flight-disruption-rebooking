<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Recommendation } from '../api'
import { formatDelay, formatLocalTime, formatMode, formatPrice, isOverDemanded } from '../format'
import OriginalJourney from './OriginalJourney.vue'
import TransportSummary from './TransportSummary.vue'

const props = defineProps<{
  recommendations: Record<string, Recommendation>
  routes: string[]
}>()

const ALL = 'ALL'
const routeFilter = ref(ALL)

const rows = computed(() =>
  Object.entries(props.recommendations)
    .map(([bookingRef, rec]) => ({
      bookingRef,
      ...rec,
      route: `${rec.journey.origin}#${rec.journey.destination}`,
      originalArrival: rec.journey.segments[rec.journey.segments.length - 1]?.arrival,
    }))
    .filter((row) => routeFilter.value === ALL || row.route === routeFilter.value),
)
</script>

<template>
  <section>
    <div class="section-header">
      <h2>Recommendations per booking</h2>
      <label>
        Route
        <select v-model="routeFilter" data-test="route-filter">
          <option :value="ALL">All routes</option>
          <option v-for="route in routes" :key="route" :value="route">{{ route.replace('#', ' → ') }}</option>
        </select>
      </label>
    </div>

    <table>
      <thead>
        <tr>
          <th>Booking</th>
          <th class="num">Pax</th>
          <th>Original journey</th>
          <th>Recommended alternative</th>
          <th>Departure</th>
          <th>Arrival</th>
          <th class="num">Price / pax</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.bookingRef" data-test="booking-row">
          <td class="mono">{{ row.bookingRef }}</td>
          <td class="num">{{ row.passengers }}</td>
          <td><OriginalJourney :journey="row.journey" /></td>
          <template v-if="row.transport">
            <td>
              <TransportSummary :transport="row.transport" />
              <div class="muted small">
                {{ formatMode(row.transport.source) }}
                <span
                  v-if="isOverDemanded(row.transport)"
                  class="badge badge-warning"
                  :title="`${row.transport.suggestedPassengers} passengers recommended for ${row.transport.totalSeats} seats`"
                >
                  high demand
                </span>
              </div>
            </td>
            <td>{{ formatLocalTime(row.transport.departure) }}</td>
            <td>
              {{ formatLocalTime(row.transport.arrival) }}
              <div v-if="row.originalArrival" class="muted small" data-test="delay">
                {{ formatDelay(row.originalArrival, row.transport.arrival) }}
              </div>
            </td>
            <td class="num">{{ formatPrice(row.transport.priceEur) }}</td>
          </template>
          <td v-else colspan="4" class="muted">No alternative with enough seats found</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
