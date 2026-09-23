<template>
  <div class="toast-host">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="`toast-${t.type}`">
      {{ t.message }}
    </div>
  </div>
</template>

<script setup>
import { toasts } from '../toast'
</script>
