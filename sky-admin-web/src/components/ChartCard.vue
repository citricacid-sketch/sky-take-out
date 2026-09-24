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
  border-radius: 4px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);

  .chart-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .chart-card-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .chart-card-body {
    width: 100%;
  }
}
</style>
