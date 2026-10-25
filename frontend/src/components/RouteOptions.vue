<script setup lang="ts">
import type { TransportOption } from '../api'
import { formatLocalTime, formatMode, formatPrice, formatRoute, isOverDemanded } from '../format'
import TransportSummary from './TransportSummary.vue'

defineProps<{ transportOptions: Record<string, TransportOption[]> }>()
</script>

<template>
  <section>
    <h2>All options per route</h2>
    <p class="muted small">
      Ranked by earliest arrival, then lower price, then source. "Recommended to" counts the passengers this option is recommended for;
      nobody is booked yet, so it can exceed the available seats.
    </p>

    <div v-for="(options, route) in transportOptions" :key="route" class="route" data-test="route">
      <h3>{{ formatRoute(String(route)) }} <span class="muted small">({{ options.length }} options)</span></h3>
      <p v-if="options.length === 0" class="muted">No source returned an option for this route.</p>
      <table v-else>
        <thead>
          <tr>
            <th>Option</th>
            <th>Source</th>
            <th>Departure</th>
            <th>Arrival</th>
            <th class="num">Price / pax</th>
            <th class="num">Seats</th>
            <th class="num">Recommended to</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="option in options" :key="option.offerId">
            <td>
              <TransportSummary :transport="option" />
            </td>
            <td>{{ formatMode(option.source) }}</td>
            <td>{{ formatLocalTime(option.departure) }}</td>
            <td>{{ formatLocalTime(option.arrival) }}</td>
            <td class="num">{{ formatPrice(option.priceEur) }}</td>
            <td class="num">{{ option.totalSeats }}</td>
            <td class="num" :class="{ warning: isOverDemanded(option) }">{{ option.suggestedPassengers }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
