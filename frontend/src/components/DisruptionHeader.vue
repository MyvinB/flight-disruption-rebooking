<script setup lang="ts">
import type { Disruption } from '../api'
import { formatLocalTime } from '../format'

defineProps<{ disruption: Disruption }>()
</script>

<template>
  <header class="disruption">
    <div>
      <h1>{{ disruption.flight }} · {{ disruption.origin }} → {{ disruption.destination }}</h1>
      <p class="muted">
        Scheduled {{ formatLocalTime(disruption.scheduledDeparture) }} –
        {{ formatLocalTime(disruption.scheduledArrival) }}
      </p>
    </div>
    <div class="disruption-facts">
      <span class="badge badge-danger">{{ disruption.status }}</span>
      <span>Reason: {{ disruption.reason }}</span>
      <span>{{ disruption.affectedBookings }} bookings · {{ disruption.affectedPassengers }} passengers</span>
    </div>
  </header>
</template>
