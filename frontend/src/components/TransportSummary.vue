<script setup lang="ts">
import type { TransportOption } from '../api'
import { formatStops, isDirect } from '../format'

defineProps<{ transport: TransportOption }>()
</script>

<template>
  <div>
    <strong>{{ transport.operator }}</strong>
    <span class="badge" :class="isDirect(transport.legs) ? 'badge-neutral' : 'badge-info'" data-test="stops">
      {{ formatStops(transport.legs) }}
    </span>
  </div>
  <ol class="leg-list" data-test="legs">
    <li v-for="(leg, i) in transport.legs" :key="i">
      <span class="mono">{{ leg.operatorCode }} {{ leg.number }}</span>
      <span>{{ leg.from }} → {{ leg.to }}</span>
      <span v-if="leg.operatorName !== transport.operator" class="muted">({{ leg.operatorName }})</span>
    </li>
  </ol>
</template>
