<script setup lang="ts">
import type { Journey } from '../api'
import { formatLocalTime, formatSegmentStatus } from '../format'

defineProps<{ journey: Journey }>()

const STATUS_CLASSES: Record<string, string> = {
  CANCELLED: 'badge-danger',
  WILL_BE_MISSED: 'badge-warning',
}
</script>

<template>
  <div>
    <strong>{{ journey.origin }} → {{ journey.destination }}</strong>
  </div>
  <ol class="leg-list" data-test="segments">
    <li v-for="segment in journey.segments" :key="segment.flight">
      <span class="mono">{{ segment.flight }}</span>
      <span>{{ segment.from }} → {{ segment.to }}</span>
      <span class="muted">{{ formatLocalTime(segment.departure) }}</span>
      <span class="badge" :class="STATUS_CLASSES[segment.status] ?? 'badge-neutral'" data-test="segment-status">
        {{ formatSegmentStatus(segment.status) }}
      </span>
    </li>
  </ol>
</template>
