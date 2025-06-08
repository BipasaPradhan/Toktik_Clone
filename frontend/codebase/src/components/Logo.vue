<template>
  <v-btn class="toc-logo" size="large" variant="text" @click="$emit('click')">
    TocTik
  </v-btn>
</template>

<script setup lang="ts">
defineEmits(['click']);
</script>

<style scoped lang="scss">
.toc-logo {
  font-size: 1.5rem !important;
  padding: 8px 16px !important;
  text-transform: none !important;
}
</style>
