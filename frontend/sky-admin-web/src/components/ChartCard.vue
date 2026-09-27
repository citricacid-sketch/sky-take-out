<template>
  <div class="chart-card">
    <div class="chart-card-header" v-if="title || $slots.header">
      <slot name="header">
        <span class="chart-card-title">{{ title }}</span>
      </slot>
    </div>
    <div class="chart-card-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
})
</script>

<style lang="scss" scoped>
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06), 0 1px 2px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);

  .chart-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }
  .chart-card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }
}
</style>
