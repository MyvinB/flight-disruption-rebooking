<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchDisruptionOptions, type DisruptionOptionsResponse } from './api'
import DisruptionHeader from './components/DisruptionHeader.vue'
import BookingsTable from './components/BookingsTable.vue'
import RouteOptions from './components/RouteOptions.vue'

const data = ref<DisruptionOptionsResponse | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const routes = computed(() => Object.keys(data.value?.transportOptions ?? {}))

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await fetchDisruptionOptions()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main>
    <div class="toolbar">
      <span class="muted small">DISRUPTION OPTIONS FINDER</span>
      <button :disabled="loading" @click="load">{{ loading ? 'Searching…' : 'Refresh' }}</button>
    </div>

    <p v-if="loading && !data" class="muted">Searching all sources…</p>
    <div v-else-if="error" class="error">
      Could not load options: {{ error }}
      <button @click="load">Try again</button>
    </div>

    <template v-if="data">
      <DisruptionHeader :disruption="data.disruption" />
      <BookingsTable :recommendations="data.recommendations" :routes="routes" />
      <RouteOptions :transport-options="data.transportOptions" />
    </template>
  </main>
</template>
