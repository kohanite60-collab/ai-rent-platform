<template>
  <NavBar />
  <main class="container">
    <router-view />
  </main>
  <ToastHost />
</template>

<script setup>
import NavBar from './components/NavBar.vue'
import ToastHost from './components/ToastHost.vue'
</script>
